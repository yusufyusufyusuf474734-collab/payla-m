package com.netshare.clients

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader
import java.net.InetAddress

data class ConnectedClient(
    val ip: String,
    val mac: String,
    val hostname: String
)

object ClientScanner {

    suspend fun scan(): List<ConnectedClient> = withContext(Dispatchers.IO) {
        val clients = mutableListOf<ConnectedClient>()
        try {
            // /proc/net/arp dosyasından bağlı cihazları oku
            BufferedReader(FileReader("/proc/net/arp")).use { reader ->
                reader.readLine() // header satırını atla
                reader.forEachLine { line ->
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size >= 4) {
                        val ip = parts[0]
                        val mac = parts[3]
                        if (mac != "00:00:00:00:00:00" && mac.length == 17) {
                            val hostname = resolveHostname(ip)
                            clients.add(ConnectedClient(ip, mac, hostname))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ClientScanner", "Scan failed", e)
        }
        clients
    }

    private fun resolveHostname(ip: String): String {
        return try {
            InetAddress.getByName(ip).canonicalHostName.takeIf { it != ip } ?: "Bilinmiyor"
        } catch (_: Exception) {
            "Bilinmiyor"
        }
    }
}
