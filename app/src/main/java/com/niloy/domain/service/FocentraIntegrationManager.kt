package com.niloy.domain.service

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.niloy.data.local.dao.FocentraStudySessionDao
import com.niloy.data.local.dao.SettingDao
import com.niloy.data.local.entity.FocentraStudySessionEntity
import com.niloy.data.local.entity.SettingEntity
import com.niloy.domain.model.FocentraIntegrationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FocentraIntegrationManager(
    private val context: Context,
    private val sessionDao: FocentraStudySessionDao,
    private val settingDao: SettingDao
) {
    companion object {
        private const val TAG = "FocentraManager"
        private const val FOCENTRA_PACKAGE = "com.focentra"
        private const val FOCENTRA_AUTHORITY = "com.focentra.provider"
        private const val SUPPORTED_SCHEMA_VERSION = 1
        
        const val PREF_CONNECTED = "focentra_connected"
        const val PREF_CONSENT = "focentra_consent_given"
        const val PREF_LAST_SYNC = "focentra_last_sync"
    }

    private val _integrationStatus = MutableStateFlow(
        FocentraIntegrationStatus(
            isInstalled = false,
            isConnected = false,
            hasConsent = false,
            schemaVersion = SUPPORTED_SCHEMA_VERSION,
            totalImportedSessions = 0,
            lastSyncTimestamp = 0L
        )
    )
    val integrationStatus: StateFlow<FocentraIntegrationStatus> = _integrationStatus.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            refreshStatus()
        }
    }

    suspend fun refreshStatus() {
        withContext(Dispatchers.IO) {
            val isInstalled = checkFocentraInstalled()
            val connectedSetting = settingDao.getByKey(PREF_CONNECTED)?.value == "true"
            val consentSetting = settingDao.getByKey(PREF_CONSENT)?.value == "true"
            val lastSync = settingDao.getByKey(PREF_LAST_SYNC)?.value?.toLongOrNull() ?: 0L
            val sessions = sessionDao.getAllSessions()

            val isConnected = isInstalled && connectedSetting && consentSetting

            _integrationStatus.value = FocentraIntegrationStatus(
                isInstalled = isInstalled,
                isConnected = isConnected,
                hasConsent = consentSetting,
                schemaVersion = SUPPORTED_SCHEMA_VERSION,
                totalImportedSessions = sessions.size,
                lastSyncTimestamp = lastSync
            )
        }
    }

    private fun checkFocentraInstalled(): Boolean {
        return try {
            val pm = context.packageManager
            pm.getPackageInfo(FOCENTRA_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            val providerInfo = context.packageManager.resolveContentProvider(FOCENTRA_AUTHORITY, 0)
            providerInfo != null
        } catch (e: Exception) {
            false
        }
    }

    suspend fun setConsent(given: Boolean) {
        withContext(Dispatchers.IO) {
            settingDao.insert(SettingEntity(PREF_CONSENT, given.toString()))
            refreshStatus()
        }
    }

    suspend fun setConnected(connected: Boolean) {
        withContext(Dispatchers.IO) {
            settingDao.insert(SettingEntity(PREF_CONNECTED, connected.toString()))
            if (connected) {
                syncSessions()
            }
            refreshStatus()
        }
    }

    suspend fun clearFocentraData() {
        withContext(Dispatchers.IO) {
            sessionDao.deleteAllSessions()
            settingDao.insert(SettingEntity(PREF_LAST_SYNC, "0"))
            refreshStatus()
        }
    }

    suspend fun syncSessions(): Int {
        return withContext(Dispatchers.IO) {
            if (!_integrationStatus.value.isConnected) {
                return@withContext 0
            }

            var importedCount = 0
            try {
                val uri = Uri.parse("content://$FOCENTRA_AUTHORITY/sessions")
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                
                cursor?.use { c ->
                    val idCol = c.getColumnIndex("sessionId")
                    val subjectCol = c.getColumnIndex("subject")
                    val topicCol = c.getColumnIndex("topic")
                    val durationCol = c.getColumnIndex("duration")
                    val startCol = c.getColumnIndex("startTime")
                    val endCol = c.getColumnIndex("endTime")
                    val compCol = c.getColumnIndex("completionStatus")
                    val scoreCol = c.getColumnIndex("focusScore")
                    val schemaCol = c.getColumnIndex("schemaVersion")

                    while (c.moveToNext()) {
                        try {
                            val schemaVersion = if (schemaCol >= 0) c.getInt(schemaCol) else 1
                            if (schemaVersion > SUPPORTED_SCHEMA_VERSION) {
                                Log.w(TAG, "Unsupported Focentra schema version: $schemaVersion")
                                continue
                            }

                            val sessionId = if (idCol >= 0) c.getString(idCol) else java.util.UUID.randomUUID().toString()
                            val subject = if (subjectCol >= 0) c.getString(subjectCol) else "Study Session"
                            val topic = if (topicCol >= 0) c.getString(topicCol) else ""
                            val startTime = if (startCol >= 0) c.getLong(startCol) else System.currentTimeMillis()
                            val endTime = if (endCol >= 0) c.getLong(endCol) else startTime
                            val duration = if (durationCol >= 0) c.getLong(durationCol) else (endTime - startTime) / 60000L
                            val completionStatus = if (compCol >= 0) c.getString(compCol) else "COMPLETED"
                            val focusScore = if (scoreCol >= 0) c.getInt(scoreCol) else 0

                            val entity = FocentraStudySessionEntity(
                                sessionId = sessionId,
                                subject = subject,
                                topic = topic,
                                startTime = startTime,
                                endTime = endTime,
                                duration = duration,
                                completionStatus = completionStatus,
                                focusScore = focusScore,
                                schemaVersion = schemaVersion
                            )

                            insertOrUpdateSession(entity)
                            importedCount++
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing Focentra session row", e)
                        }
                    }
                }

                val now = System.currentTimeMillis()
                settingDao.insert(SettingEntity(PREF_LAST_SYNC, now.toString()))
                sendOutboundProductivityContext()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync Focentra sessions via ContentProvider", e)
            }

            refreshStatus()
            importedCount
        }
    }

    suspend fun getAllSessions(): List<FocentraStudySessionEntity> {
        return withContext(Dispatchers.IO) {
            sessionDao.getAllSessions()
        }
    }

    suspend fun insertOrUpdateSessions(sessions: List<FocentraStudySessionEntity>) {
        withContext(Dispatchers.IO) {
            sessionDao.insertSessions(sessions)
            refreshStatus()
        }
    }

    suspend fun insertOrUpdateSession(session: FocentraStudySessionEntity) {
        withContext(Dispatchers.IO) {
            sessionDao.insertSession(session)
            refreshStatus()
        }
    }

    private fun sendOutboundProductivityContext() {
        try {
            val intent = android.content.Intent("com.focentra.ACTION_SYNC_CONTEXT").apply {
                setPackage(FOCENTRA_PACKAGE)
                putExtra("sourceApp", "Daynexa")
                putExtra("version", "0.5.0")
                putExtra("timestamp", System.currentTimeMillis())
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            // Ignored if Focentra not listening
        }
    }
}
