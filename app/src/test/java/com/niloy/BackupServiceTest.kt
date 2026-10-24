package com.niloy

import com.niloy.domain.model.Category
import com.niloy.domain.model.Task
import com.niloy.domain.service.BackupData
import com.niloy.domain.service.BackupService
import com.niloy.domain.service.BackupValidationResult
import com.niloy.domain.service.FocentraExportBundle
import com.niloy.domain.service.FocentraSessionBackup
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BackupServiceTest {

    private lateinit var backupService: BackupService

    @Before
    fun setUp() {
        backupService = BackupService()
    }

    @Test
    fun exportBackup_generatesValidJsonWithChecksumAndAppVersion() {
        val testCategories = listOf(Category(id = 1, name = "Health", icon = "fitness", color = 0xFF10B981.toInt()))
        val testTasks = listOf(Task(id = 1, name = "Morning Exercise", categoryId = 1))
        val data = BackupData(
            backupVersion = 5,
            appVersion = "3.0.0",
            categories = testCategories,
            tasks = testTasks
        )

        val json = backupService.exportBackup(data)
        assertNotNull(json)
        assertTrue(json.contains("\"backupVersion\": 5"))
        assertTrue(json.contains("\"appVersion\": \"3.0.0\""))
        assertTrue(json.contains("\"checksum\":"))

        val validation = backupService.validateBackup(json)
        assertTrue(validation is BackupValidationResult.ValidFull)
        val validResult = validation as BackupValidationResult.ValidFull
        assertEquals("3.0.0", validResult.data.appVersion)
        assertEquals(1, validResult.preview.tasksCount)
        assertEquals(1, validResult.preview.categoriesCount)
    }

    @Test
    fun validateBackup_corruptedChecksum_returnsInvalidResult() {
        val data = BackupData(
            backupVersion = 5,
            appVersion = "3.0.0",
            categories = listOf(Category(id = 1, name = "Work", icon = "code", color = 0)),
            tasks = listOf(Task(id = 1, name = "Deep Work", categoryId = 1))
        )
        val json = backupService.exportBackup(data)

        // Tamper with tasks count or checksum in json string
        val tamperedJson = json.replace("\"tasks\": [", "\"tasks\": [ {\"id\": 99, \"name\": \"Tampered Task\", \"categoryId\": 1},")

        val validation = backupService.validateBackup(tamperedJson)
        assertTrue(validation is BackupValidationResult.Invalid)
        val invalidResult = validation as BackupValidationResult.Invalid
        assertTrue(invalidResult.errorReason.contains("integrity") || invalidResult.errorReason.contains("Checksum"))
    }

    @Test
    fun validateBackup_emptyOrMalformedJson_returnsInvalidResult() {
        val emptyValidation = backupService.validateBackup("")
        assertTrue(emptyValidation is BackupValidationResult.Invalid)

        val malformedValidation = backupService.validateBackup("{ invalid_json: true ")
        assertTrue(malformedValidation is BackupValidationResult.Invalid)
    }

    @Test
    fun validateBackup_focentraBundle_returnsValidFocentraResult() {
        val bundle = FocentraExportBundle(
            exportVersion = 1,
            appVersion = "3.0.0",
            sessions = listOf(
                FocentraSessionBackup(
                    sessionId = "foc_101",
                    subject = "Math",
                    topic = "Algebra",
                    startTime = 1000L,
                    endTime = 2000L,
                    duration = 1000L,
                    completionStatus = "COMPLETED",
                    focusScore = 95
                )
            )
        )

        val json = backupService.exportFocentraBundle(bundle)
        val validation = backupService.validateBackup(json)
        assertTrue("Expected ValidFocentra but got $validation", validation is BackupValidationResult.ValidFocentra)
        val focentraResult = validation as BackupValidationResult.ValidFocentra
        assertEquals(1, focentraResult.preview.focentraSessionsCount)
    }
}
