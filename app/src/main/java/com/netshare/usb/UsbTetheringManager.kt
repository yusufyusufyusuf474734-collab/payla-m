package com.netshare.usb

import android.util.Log

object UsbTetheringManager {

    // Root ile USB tethering aç/kapat
    fun setEnabled(enabled: Boolean): Boolean {
        return try {
            val cmd = if (enabled)
                "svc usb setFunctions rndis"
            else
                "svc usb setFunctions"

            Runtime.getRuntime().exec(arrayOf("su", "-c", cmd)).waitFor()
            Log.d("UsbTether", "USB tethering ${if (enabled) "enabled" else "disabled"}")
            true
        } catch (e: Exception) {
            Log.e("UsbTether", "Failed", e)
            false
        }
    }

    fun isEnabled(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "svc usb getFunctions"))
            val output = process.inputStream.bufferedReader().readText()
            output.contains("rndis")
        } catch (e: Exception) {
            false
        }
    }
}
