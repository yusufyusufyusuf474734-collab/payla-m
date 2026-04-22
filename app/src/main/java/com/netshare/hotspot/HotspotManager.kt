package com.netshare.hotspot

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.util.Log

class HotspotManager(private val context: Context) {

    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null
    private val wifiManager = context.getSystemService(WifiManager::class.java)

    var onStarted: ((ssid: String, password: String) -> Unit)? = null
    var onStopped: (() -> Unit)? = null

    fun start() {
        wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
            override fun onStarted(r: WifiManager.LocalOnlyHotspotReservation) {
                reservation = r
                val config = r.wifiConfiguration
                val ssid = config?.SSID ?: "NetShare"
                val pass = config?.preSharedKey ?: ""
                Log.d("Hotspot", "Started: $ssid / $pass")
                onStarted?.invoke(ssid, pass)
            }

            override fun onStopped() {
                reservation = null
                onStopped?.invoke()
            }

            override fun onFailed(reason: Int) {
                Log.e("Hotspot", "Failed: $reason")
            }
        }, Handler(Looper.getMainLooper()))
    }

    fun stop() {
        reservation?.close()
        reservation = null
        onStopped?.invoke()
    }

    fun isActive() = reservation != null
}
