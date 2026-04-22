package com.netshare.nat

import android.util.Log

object NatManager {

    private const val TAG = "NatManager"

    // Hotspot interface genellikle wlan1 veya ap0, USB tethering rndis0
    fun enable(outInterface: String, inInterface: String) {
        runRootCommands(
            // IP forwarding aç
            "echo 1 > /proc/sys/net/ipv4/ip_forward",
            // Gelen trafiği internete yönlendir (MASQUERADE = dinamik NAT)
            "iptables -t nat -A POSTROUTING -o $outInterface -j MASQUERADE",
            // Forward zinciri — gelen ve giden trafiğe izin ver
            "iptables -A FORWARD -i $inInterface -o $outInterface -j ACCEPT",
            "iptables -A FORWARD -i $outInterface -o $inInterface -m state --state RELATED,ESTABLISHED -j ACCEPT"
        )
        Log.d(TAG, "NAT enabled: $inInterface -> $outInterface")
    }

    fun disable(outInterface: String, inInterface: String) {
        runRootCommands(
            "iptables -t nat -D POSTROUTING -o $outInterface -j MASQUERADE",
            "iptables -D FORWARD -i $inInterface -o $outInterface -j ACCEPT",
            "iptables -D FORWARD -i $outInterface -o $inInterface -m state --state RELATED,ESTABLISHED -j ACCEPT",
            "echo 0 > /proc/sys/net/ipv4/ip_forward"
        )
        Log.d(TAG, "NAT disabled")
    }

    fun detectMobileInterface(): String {
        // Aktif mobil veri interface'ini bul (rmnet0, wwan0, vs.)
        return try {
            val result = Runtime.getRuntime()
                .exec(arrayOf("su", "-c", "ip route show default"))
                .inputStream.bufferedReader().readText()
            // "default via X.X.X.X dev rmnet0" satırından interface'i çek
            result.lines()
                .firstOrNull { it.contains("default") }
                ?.split(" ")
                ?.let { parts -> parts.getOrNull(parts.indexOf("dev") + 1) }
                ?: "rmnet0"
        } catch (_: Exception) { "rmnet0" }
    }

    private fun runRootCommands(vararg commands: String) {
        commands.forEach { cmd ->
            try {
                Runtime.getRuntime().exec(arrayOf("su", "-c", cmd)).waitFor()
            } catch (e: Exception) {
                Log.e(TAG, "Command failed: $cmd", e)
            }
        }
    }
}
