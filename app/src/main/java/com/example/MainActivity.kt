package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.JarvisViewModel
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.JarvisMainScreen
import com.example.ui.screens.PcControlScreen
import com.example.ui.screens.SetupGuideScreen
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.MyApplicationTheme

enum class JarvisTab(val title: String, val icon: ImageVector, val tag: String) {
    VOICE_HUD("Jarvis", Icons.Default.Mic, "tab_voice_hud"),
    PC_SYSTEM("PC Control", Icons.Default.Computer, "tab_pc_control"),
    HISTORY("History", Icons.Default.History, "tab_history"),
    DIAGNOSTICS("Diagnostics", Icons.Default.Build, "tab_diagnostics"),
    SETUP("Guide", Icons.Default.Settings, "tab_setup")
}

class MainActivity : ComponentActivity() {

    private var jarvisViewModel: JarvisViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val viewModel: JarvisViewModel = viewModel()
                jarvisViewModel = viewModel

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    viewModel.refreshPermissionStatus()
                }

                fun requestPermissions() {
                    val permissions = mutableListOf(
                        android.Manifest.permission.RECORD_AUDIO,
                        android.Manifest.permission.READ_CONTACTS,
                        android.Manifest.permission.CALL_PHONE,
                        android.Manifest.permission.READ_CALENDAR,
                        android.Manifest.permission.CAMERA
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(permissions.toTypedArray())
                }

                fun openAccessibilitySettings() {
                    try {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                LaunchedEffect(Unit) {
                    requestPermissions()
                    // Check if launched from wake word background service
                    val wakeCommand = intent.getStringExtra("EXTRA_WAKE_COMMAND")
                    if (!wakeCommand.isNullOrEmpty()) {
                        viewModel.processVoiceCommand(wakeCommand)
                    }
                }

                var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

                BackHandler(enabled = selectedTabIndex != 0) {
                    selectedTabIndex = 0
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF060A14),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .testTag("main_bottom_nav"),
                            containerColor = JarvisSurface,
                            tonalElevation = 8.dp
                        ) {
                            JarvisTab.values().forEachIndexed { index, tab ->
                                NavigationBarItem(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.title
                                        )
                                    },
                                    label = {
                                        Text(text = tab.title, fontSize = 10.sp)
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = JarvisCyan,
                                        indicatorColor = JarvisCyan,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    ),
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTabIndex) {
                            0 -> JarvisMainScreen(
                                viewModel = viewModel,
                                onRequestPermissions = { requestPermissions() },
                                onOpenAccessibilitySettings = { openAccessibilitySettings() }
                            )
                            1 -> PcControlScreen(
                                viewModel = viewModel
                            )
                            2 -> HistoryScreen(
                                viewModel = viewModel
                            )
                            3 -> DiagnosticsScreen(
                                viewModel = viewModel,
                                onOpenAccessibilitySettings = { openAccessibilitySettings() }
                            )
                            4 -> SetupGuideScreen(
                                onOpenAccessibilitySettings = { openAccessibilitySettings() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val wakeCommand = intent.getStringExtra("EXTRA_WAKE_COMMAND")
        if (!wakeCommand.isNullOrEmpty()) {
            jarvisViewModel?.processVoiceCommand(wakeCommand)
        }
    }
}
