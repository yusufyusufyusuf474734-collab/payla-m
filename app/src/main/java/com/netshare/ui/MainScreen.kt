package com.netshare.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netshare.clients.ConnectedClient

@Composable
fun MainScreen(
    vpnActive: Boolean,
    hotspotActive: Boolean,
    usbActive: Boolean,
    hotspotInfo: Pair<String, String>?,
    clients: List<ConnectedClient>,
    onVpnToggle: (Boolean) -> Unit,
    onHotspotToggle: (Boolean) -> Unit,
    onUsbToggle: (Boolean) -> Unit,
    onRefreshClients: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("NetShare", style = MaterialTheme.typography.headlineMedium)
        }

        item {
            ToggleCard(title = "VPN Tüneli", checked = vpnActive, onToggle = onVpnToggle) {
                if (vpnActive) StatusText("TCP + UDP aktif")
            }
        }

        item {
            ToggleCard(title = "WiFi Hotspot", checked = hotspotActive, onToggle = onHotspotToggle) {
                if (hotspotActive && hotspotInfo != null) {
                    StatusText("SSID: ${hotspotInfo.first}")
                    StatusText("Şifre: ${hotspotInfo.second}")
                }
            }
        }

        item {
            ToggleCard(title = "USB Tethering", checked = usbActive, onToggle = onUsbToggle) {
                if (usbActive) StatusText("Aktif (Root)")
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bağlı Cihazlar (${clients.size})")
                        TextButton(onClick = onRefreshClients) { Text("Yenile") }
                    }
                    if (clients.isEmpty()) {
                        Text("Cihaz bulunamadı", style = MaterialTheme.typography.bodySmall)
                    } else {
                        clients.forEach { client ->
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(client.hostname, style = MaterialTheme.typography.bodyMedium)
                            Text("${client.ip}  •  ${client.mac}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleCard(
    title: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title)
                Switch(checked = checked, onCheckedChange = onToggle)
            }
            content()
        }
    }
}

@Composable
private fun StatusText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
}
