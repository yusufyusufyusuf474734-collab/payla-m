package com.netshare.nat

import android.util.Log
import java.net.NetworkInterface

object NatManager {

    private const val TAG = "NatManager"

    fun enable(inInterface: String) {
        val out = detectMobileInterface()
        runRoot(
            "echo 1 > /proc/sys/net/ipv4/ip_forward",
            "iptables -t nat -A POSTROUTING -o $out -j MASQUERADE",
            "iptables -A FORWARD -i $inInterface -o $out -j ACCEPT",
            "iptables -A FORWARD -i $out -o $inInterface -m state --state RELATED,ESTABLISHED -j ACCEPT"
        )
        Log.d(TAG, "NAT enabled: $inInterface -> $out")
    }

    fun disable(inInterface: String) {
        val out = detectMobileInterface()
        runRoot(
            "iptables -t nat -D POSTROUTING -o $out -j MASQUERADE",
            "iptables -D FORWARD -i $inInterface -o $out -j ACCEPT",
            "iptables -D FORWARD -i $out -o $inInterface -m state --state RELATED,ESTABLISHED -j ACCEPT"
        )
    }

    fun detectHotspotInterface(): String =
        NetworkInterface.getNetworkInterfaces()?.toList()
            ?.firstOrNull { it.isUp && !it.isLoopback && (it.name.startsWith("ap") || it.name.startsWith("wlan1") || it.name.startsWith("softap")) }
            ?.name ?: "ap0"

    fun detectUsbInterface(): String =
        NetworkInterface.getNetworkInterfaces()?.toList()
            ?.firstOrNull { it.isUp && !it.isLoopback && (it.name.startsWith("rndis") || it.name.startsWith("usb")) }
            ?.name ?: "rndis0"

    fun detectMobileInterface(): String =
        try {
            Runtime.getRuntime().exec(arrayOf("su", "-c", "ip route show default"))
                .inputStream.bufferedReader().readText()
                .lines().firstOrNull { it.contains("default") }
                ?.split(" ")?.let { it.getOrNull(it.indexOf("dev") + 1) } ?: "rmnet0"
        } catch (_: Exception) { "rmnet0" }

    private fun runRoot(vararg cmds: String) = cmds.forEach {
        try { Runtime.getRuntime().exec(arrayOf("su", "-c", it)).waitFor() }
        catch (e: Exception) { Log.e(TAG, "Failed: $it", e) }
    }
}
