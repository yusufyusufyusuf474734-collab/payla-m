package com.netshare.vpn

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.nio.ByteBuffer

class NetShareVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var running = false

    companion object {
        const val TAG = "NetShareVPN"
        const val VPN_ADDRESS = "10.0.0.2"
        const val VPN_ROUTE = "0.0.0.0"
        const val VPN_DNS = "8.8.8.8"
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopVpn()
            return START_NOT_STICKY
        }
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        val notification = buildNotification()
        startForeground(1, notification)

        vpnInterface = Builder()
            .addAddress(VPN_ADDRESS, 24)
            .addRoute(VPN_ROUTE, 0)
            .addDnsServer(VPN_DNS)
            .setSession("NetShare")
            .establish()

        running = true
        Thread { forwardPackets() }.start()
        Log.d(TAG, "VPN started")
    }

    private fun forwardPackets() {
        val input = FileInputStream(vpnInterface!!.fileDescriptor)
        val output = FileOutputStream(vpnInterface!!.fileDescriptor)
        val buffer = ByteBuffer.allocate(32767)

        while (running) {
            try {
                val length = input.read(buffer.array())
                if (length > 0) {
                    buffer.limit(length)
                    PacketForwarder.forward(buffer, output, this)
                    buffer.clear()
                }
            } catch (e: Exception) {
                if (running) Log.e(TAG, "Packet error", e)
            }
        }
    }

    private fun stopVpn() {
        running = false
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(true)
        stopSelf()
        Log.d(TAG, "VPN stopped")
    }

    private fun buildNotification(): android.app.Notification {
        val channelId = "netshare_vpn"
        val manager = getSystemService(android.app.NotificationManager::class.java)
        val channel = android.app.NotificationChannel(
            channelId, "NetShare VPN", android.app.NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)

        return android.app.Notification.Builder(this, channelId)
            .setContentTitle("NetShare Aktif")
            .setContentText("İnternet paylaşımı devam ediyor")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .build()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
