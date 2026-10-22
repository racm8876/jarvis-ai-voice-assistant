package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PcAction
import com.example.ui.JarvisViewModel
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisLime
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSurfaceCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PcControlScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val pcConfig by viewModel.pcConfig.collectAsState()

    var hostInput by remember(pcConfig.host) { mutableStateOf(pcConfig.host) }
    var portInput by remember(pcConfig.port) { mutableStateOf(pcConfig.port.toString()) }
    var tokenInput by remember(pcConfig.authToken) { mutableStateOf(pcConfig.authToken) }
    var isEnabled by remember(pcConfig.isEnabled) { mutableStateOf(pcConfig.isEnabled) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF060A14))
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Computer,
                    contentDescription = null,
                    tint = JarvisCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "WINDOWS PC & SYSTEM CONTROL",
                        color = JarvisCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "FastAPI Bridge, Device Toggles & Wake Word",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 1. Background Wake-Word Service Toggle
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("wake_word_service_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (uiState.isBackgroundServiceRunning) JarvisLime else Color(0xFF1B2C47))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (uiState.isBackgroundServiceRunning) JarvisLime.copy(alpha = 0.15f) else Color(0xFF16233B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hearing,
                                contentDescription = null,
                                tint = if (uiState.isBackgroundServiceRunning) JarvisLime else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Background \"Hi Jarvis\" Wake Word",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (uiState.isBackgroundServiceRunning) "Foreground Service Active • Auto-reboot ready" else "Disabled • Tap switch to activate",
                                color = if (uiState.isBackgroundServiceRunning) JarvisLime else Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = uiState.isBackgroundServiceRunning,
                        onCheckedChange = { viewModel.toggleBackgroundService() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = JarvisLime,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF101B30)
                        ),
                        modifier = Modifier.testTag("wake_word_switch")
                    )
                }
            }
        }

        // Battery Optimization Exemption Card
        item {
            val context = androidx.compose.ui.platform.LocalContext.current
            val activity = context as? android.app.Activity

            Card(
                modifier = Modifier.fillMaxWidth().testTag("battery_opt_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (uiState.isBatteryOptimizationExempt) JarvisLime.copy(alpha = 0.5f) else JarvisAmber.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Battery Optimization (Doze Mode)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (uiState.isBatteryOptimizationExempt)
                                "Exempt: Background microphone active 24/7 without Android sleep"
                            else
                                "Restricted: Android may pause listening when screen is off",
                            color = if (uiState.isBatteryOptimizationExempt) JarvisLime else JarvisAmber,
                            fontSize = 11.sp
                        )
                    }

                    if (!uiState.isBatteryOptimizationExempt && activity != null) {
                        Button(
                            onClick = { viewModel.requestDisableBatteryOptimization(activity) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisAmber,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("disable_battery_opt_button")
                        ) {
                            Text("Exempt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Phone System Hardware Controls (Torch, Volume, Settings, Files)
        item {
            Text(
                text = "PHONE SYSTEM HARDWARE CONTROLS",
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B2C47))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Torch Toggle
                        QuickSystemButton(
                            title = if (uiState.isTorchOn) "Torch ON" else "Torch OFF",
                            icon = if (uiState.isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            tint = if (uiState.isTorchOn) JarvisGold else Color.Gray,
                            onClick = { viewModel.systemControl.toggleTorch() }
                        )

                        // Volume Up
                        QuickSystemButton(
                            title = "Volume +",
                            icon = Icons.Default.VolumeUp,
                            tint = JarvisCyan,
                            onClick = { viewModel.systemControl.adjustVolume(true) }
                        )

                        // Volume Down
                        QuickSystemButton(
                            title = "Volume -",
                            icon = Icons.Default.VolumeDown,
                            tint = JarvisCyan,
                            onClick = { viewModel.systemControl.adjustVolume(false) }
                        )

                        // Mute
                        QuickSystemButton(
                            title = "Mute",
                            icon = Icons.Default.VolumeMute,
                            tint = JarvisAmber,
                            onClick = { viewModel.systemControl.setMute(true) }
                        )

                        // Wi-Fi Panel
                        QuickSystemButton(
                            title = "Wi-Fi Panel",
                            icon = Icons.Default.Wifi,
                            tint = JarvisLime,
                            onClick = { viewModel.systemControl.openSystemSettings("wifi") }
                        )

                        // File Browser
                        QuickSystemButton(
                            title = "Files",
                            icon = Icons.Default.Folder,
                            tint = JarvisCyan,
                            onClick = { viewModel.systemControl.openFileManager() }
                        )
                    }
                }
            }
        }

        // 3. Windows PC FastAPI Companion Bridge Configuration
        item {
            Text(
                text = "WINDOWS PC FASTAPI BRIDGE CONFIGURATION",
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("pc_bridge_config_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B2C47))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Enable PC Remote Control",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = {
                                isEnabled = it
                                viewModel.updatePcConfig(
                                    pcConfig.copy(
                                        host = hostInput,
                                        port = portInput.toIntOrNull() ?: 8000,
                                        authToken = tokenInput,
                                        isEnabled = it
                                    )
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = JarvisCyan
                            )
                        )
                    }

                    // Host IP & Port
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = hostInput,
                            onValueChange = { hostInput = it },
                            label = { Text("PC IP Address") },
                            modifier = Modifier.weight(2f).testTag("pc_host_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisCyan,
                                unfocusedBorderColor = Color(0xFF1E355B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = portInput,
                            onValueChange = { portInput = it },
                            label = { Text("Port") },
                            modifier = Modifier.weight(1f).testTag("pc_port_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisCyan,
                                unfocusedBorderColor = Color(0xFF1E355B),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )
                    }

                    // Auth Token (Bearer token, never plain password)
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("Bearer Auth Token") },
                        modifier = Modifier.fillMaxWidth().testTag("pc_token_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = Color(0xFF1E355B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.updatePcConfig(
                                    pcConfig.copy(
                                        host = hostInput,
                                        port = portInput.toIntOrNull() ?: 8000,
                                        authToken = tokenInput,
                                        isEnabled = isEnabled
                                    )
                                )
                                viewModel.pingPc()
                            },
                            modifier = Modifier.weight(1f).height(44.dp).testTag("save_ping_pc_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save & Ping PC", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Live Status
                    Surface(
                        color = Color(0xFF091222),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Text(
                            text = "Status: ${uiState.pcConnectionStatus}",
                            color = JarvisCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // 4. Authorized Windows PC Quick Actions
        item {
            Text(
                text = "AUTHORIZED WINDOWS PC ACTIONS",
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B2C47))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickSystemButton(
                            title = "Lock PC",
                            icon = Icons.Default.Lock,
                            tint = JarvisAmber,
                            onClick = { viewModel.processVoiceCommand("Lock my PC") }
                        )

                        QuickSystemButton(
                            title = "Play / Pause",
                            icon = Icons.Default.PlayArrow,
                            tint = JarvisCyan,
                            onClick = { viewModel.processVoiceCommand("Play media on PC") }
                        )

                        QuickSystemButton(
                            title = "Next Track",
                            icon = Icons.Default.SkipNext,
                            tint = JarvisCyan,
                            onClick = { viewModel.processVoiceCommand("Next song on PC") }
                        )

                        QuickSystemButton(
                            title = "PC Vol +",
                            icon = Icons.Default.VolumeUp,
                            tint = JarvisLime,
                            onClick = { viewModel.processVoiceCommand("Volume up on PC") }
                        )

                        QuickSystemButton(
                            title = "PC Vol -",
                            icon = Icons.Default.VolumeDown,
                            tint = JarvisLime,
                            onClick = { viewModel.processVoiceCommand("Volume down on PC") }
                        )

                        QuickSystemButton(
                            title = "Mute PC",
                            icon = Icons.Default.VolumeMute,
                            tint = JarvisAmber,
                            onClick = { viewModel.processVoiceCommand("Mute PC") }
                        )

                        QuickSystemButton(
                            title = "Open Chrome",
                            icon = Icons.Default.Computer,
                            tint = JarvisCyan,
                            onClick = { viewModel.processVoiceCommand("Open Chrome on PC") }
                        )

                        QuickSystemButton(
                            title = "Shutdown PC",
                            icon = Icons.Default.PowerSettingsNew,
                            tint = JarvisRed,
                            onClick = { viewModel.processVoiceCommand("Shutdown PC") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSystemButton(
    title: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF091222),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E355B)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = Modifier.testTag("quick_sys_btn_${title.take(10).replace(" ", "_")}")
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
