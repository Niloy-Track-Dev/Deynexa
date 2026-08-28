package com.niloy.domain.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.niloy.DaynexaApplication
import com.niloy.MainActivity
import com.niloy.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class WebsiteDiagnosticVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.niloy.daynexa.vpn.ACTION_START"
        const val ACTION_STOP = "com.niloy.daynexa.vpn.ACTION_STOP"
        const val CHANNEL_ID = "daynexa_vpn_diagnostics"
        const val NOTIFICATION_ID = 2001

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, WebsiteDiagnosticVpnService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, WebsiteDiagnosticVpnService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        if (action == ACTION_STOP) {
            stopVpn()
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        startVpn()

        return START_STICKY
    }

    private fun startVpn() {
        if (_isRunning.value) return

        try {
            val builder = Builder()
                .setSession("Daynexa Website Diagnostics")
                .addAddress("10.1.10.1", 24)
                .addDnsServer("8.8.8.8")
                .addRoute("8.8.8.8", 32)
                .addDnsServer("1.1.1.1")
                .addRoute("1.1.1.1", 32)
                .setMtu(1500)
                .setBlocking(true)

            vpnInterface = builder.establish()

            if (vpnInterface != null) {
                _isRunning.value = true
                vpnJob = serviceScope.launch {
                    runPacketLoop(vpnInterface!!)
                }
            } else {
                stopSelf()
            }
        } catch (e: Exception) {
            stopVpn()
            stopSelf()
        }
    }

    private suspend fun runPacketLoop(descriptor: ParcelFileDescriptor) {
        val inStream = FileInputStream(descriptor.fileDescriptor)
        val outStream = FileOutputStream(descriptor.fileDescriptor)
        val packet = ByteBuffer.allocate(32767)

        val app = application as? DaynexaApplication
        val websiteRepo = app?.websiteDiagnosticRepository

        var dnsSocket: DatagramSocket? = null
        try {
            dnsSocket = DatagramSocket()
            protect(dnsSocket)
            dnsSocket.soTimeout = 2500

            val upstreamDns = InetAddress.getByName("8.8.8.8")

            withContext(Dispatchers.IO) {
                val buffer = ByteArray(32767)
                while (isActive && _isRunning.value) {
                    val length = try {
                        inStream.read(buffer)
                    } catch (e: Exception) {
                        break
                    }

                    if (length <= 0) continue

                    // Parse IPv4 packet
                    if (length > 28 && (buffer[0].toInt() and 0xF0) == 0x40) {
                        val protocol = buffer[9].toInt() and 0xFF
                        val ipHeaderLength = (buffer[0].toInt() and 0x0F) * 4

                        // UDP Protocol
                        if (protocol == 17 && length >= ipHeaderLength + 8) {
                            val srcPort = ((buffer[ipHeaderLength].toInt() and 0xFF) shl 8) or (buffer[ipHeaderLength + 1].toInt() and 0xFF)
                            val dstPort = ((buffer[ipHeaderLength + 2].toInt() and 0xFF) shl 8) or (buffer[ipHeaderLength + 3].toInt() and 0xFF)

                            // DNS query on port 53
                            if (dstPort == 53) {
                                val udpPayloadOffset = ipHeaderLength + 8
                                val udpPayloadLength = length - udpPayloadOffset

                                if (udpPayloadLength > 12) {
                                    val domain = extractDomainFromDnsQuery(buffer, udpPayloadOffset, udpPayloadLength)
                                    if (domain != null && domain.isNotBlank()) {
                                        websiteRepo?.recordDomainVisit(domain)
                                    }

                                    // Forward DNS query upstream
                                    try {
                                        val outPacket = DatagramPacket(buffer, udpPayloadOffset, udpPayloadLength, upstreamDns, 53)
                                        dnsSocket.send(outPacket)

                                        val responseBuf = ByteArray(2048)
                                        val inPacket = DatagramPacket(responseBuf, responseBuf.size)
                                        dnsSocket.receive(inPacket)

                                        // Build response IPv4 + UDP packet back to TUN
                                        val respPacket = buildIpUdpPacket(
                                            srcIp = byteArrayOf(8, 8, 8, 8),
                                            dstIp = byteArrayOf(buffer[12], buffer[13], buffer[14], buffer[15]),
                                            srcPort = 53,
                                            dstPort = srcPort,
                                            payload = inPacket.data,
                                            payloadLength = inPacket.length
                                        )
                                        outStream.write(respPacket)
                                    } catch (e: Exception) {
                                        // Ignore transient DNS upstream timeout
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Service loop ended
        } finally {
            try { dnsSocket?.close() } catch (e: Exception) {}
            try { inStream.close() } catch (e: Exception) {}
            try { outStream.close() } catch (e: Exception) {}
        }
    }

    private fun extractDomainFromDnsQuery(buffer: ByteArray, offset: Int, length: Int): String? {
        try {
            // DNS Header is 12 bytes. Question section starts at offset + 12
            var pos = offset + 12
            val end = offset + length
            val domainBuilder = StringBuilder()

            while (pos < end) {
                val labelLength = buffer[pos].toInt() and 0xFF
                pos++
                if (labelLength == 0) break // Root null label terminates question name
                if (labelLength > 63 || pos + labelLength > end) return null

                if (domainBuilder.isNotEmpty()) domainBuilder.append(".")
                val label = String(buffer, pos, labelLength, Charsets.US_ASCII)
                domainBuilder.append(label)
                pos += labelLength
            }
            return domainBuilder.toString()
        } catch (e: Exception) {
            return null
        }
    }

    private fun buildIpUdpPacket(
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        payload: ByteArray,
        payloadLength: Int
    ): ByteArray {
        val totalLength = 20 + 8 + payloadLength
        val packet = ByteArray(totalLength)

        // IP Header (20 bytes)
        packet[0] = 0x45.toByte() // IPv4, Header length 5 (20 bytes)
        packet[1] = 0x00.toByte() // DSCP/ECN
        packet[2] = ((totalLength shr 8) and 0xFF).toByte()
        packet[3] = (totalLength and 0xFF).toByte()
        packet[4] = 0x00.toByte() // ID
        packet[5] = 0x00.toByte()
        packet[6] = 0x40.toByte() // Flags: Don't Fragment
        packet[7] = 0x00.toByte()
        packet[8] = 64.toByte()   // TTL
        packet[9] = 17.toByte()   // Protocol: UDP
        // Checksum at 10,11 (set later)
        System.arraycopy(srcIp, 0, packet, 12, 4)
        System.arraycopy(dstIp, 0, packet, 16, 4)

        // Calculate IP checksum
        var sum = 0
        for (i in 0 until 10) {
            val w = ((packet[i * 2].toInt() and 0xFF) shl 8) or (packet[i * 2 + 1].toInt() and 0xFF)
            sum += w
        }
        while ((sum shr 16) > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        val ipChecksum = sum.inv() and 0xFFFF
        packet[10] = ((ipChecksum shr 8) and 0xFF).toByte()
        packet[11] = (ipChecksum and 0xFF).toByte()

        // UDP Header (8 bytes)
        val udpOffset = 20
        packet[udpOffset] = ((srcPort shr 8) and 0xFF).toByte()
        packet[udpOffset + 1] = (srcPort and 0xFF).toByte()
        packet[udpOffset + 2] = ((dstPort shr 8) and 0xFF).toByte()
        packet[udpOffset + 3] = (dstPort and 0xFF).toByte()
        val udpLength = 8 + payloadLength
        packet[udpOffset + 4] = ((udpLength shr 8) and 0xFF).toByte()
        packet[udpOffset + 5] = (udpLength and 0xFF).toByte()
        packet[udpOffset + 6] = 0x00.toByte() // UDP checksum optional in IPv4
        packet[udpOffset + 7] = 0x00.toByte()

        // UDP Payload
        System.arraycopy(payload, 0, packet, udpOffset + 8, payloadLength)
        return packet
    }

    private fun stopVpn() {
        _isRunning.value = false
        vpnJob?.cancel()
        vpnJob = null
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            // Ignore close error
        }
    }

    override fun onDestroy() {
        stopVpn()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        stopSelf()
        super.onRevoke()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Website Diagnostics Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors local domain activity with zero cloud telemetry"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, WebsiteDiagnosticVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Daynexa Website Diagnostics")
            .setContentText("Local domain activity diagnostic is active (Privacy-First)")
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_notification, "Stop Diagnostics", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
