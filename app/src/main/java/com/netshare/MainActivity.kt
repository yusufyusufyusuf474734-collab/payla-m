package com.netshare

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.netshare.clients.ClientScanner
import com.netshare.clients.ConnectedClient
import com.netshare.hotspot.HotspotManager
import com.netshare.ui.MainScreen
import com.netshare.usb.UsbTetheringManager
import com.netshare.vpn.NetShareVpnService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var hotspotManager: HotspotManager

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) startVpnService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hotspotManager = HotspotManager(this)

        setContent {
            MaterialTheme {
                var vpnActive by remember { mutableStateOf(false) }
                var hotspotActive by remember { mutableStateOf(false) }
                var usbActive by remember { mutableStateOf(false) }
                var hotspotInfo by remember { mutableStateOf<Pair<String, String>?>(null) }
                var clients by remember { mutableStateOf<List<ConnectedClient>>(emptyList()) }

                hotspotManager.onStarted = { ssid, pass ->
                    hotspotInfo = Pair(ssid, pass)
                    hotspotActive = true
                }
                hotspotManager.onStopped = {
                    hotspotActive = false
                    hotspotInfo = null
                }

                MainScreen(
                    vpnActive = vpnActive,
                    hotspotActive = hotspotActive,
                    usbActive = usbActive,
                    hotspotInfo = hotspotInfo,
                    clients = clients,
                    onVpnToggle = { enabled ->
                        if (enabled) requestVpnPermission() else stopVpnService()
                        vpnActive = enabled
                    },
                    onHotspotToggle = { enabled ->
                        if (enabled) hotspotManager.start() else hotspotManager.stop()
                    },
                    onUsbToggle = { enabled ->
                        if (UsbTetheringManager.setEnabled(enabled)) usbActive = enabled
                    },
                    onRefreshClients = {
                        lifecycleScope.launch {
                            clients = ClientScanner.scan()
                        }
                    }
                )
            }
        }
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) vpnPermissionLauncher.launch(intent) else startVpnService()
    }

    private fun startVpnService() =
        startService(Intent(this, NetShareVpnService::class.java))

    private fun stopVpnService() =
        startService(Intent(this, NetShareVpnService::class.java).apply { action = "STOP" })
}
