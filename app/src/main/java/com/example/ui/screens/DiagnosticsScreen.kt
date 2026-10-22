package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.JarvisViewModel
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisLime
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSurfaceCard
import kotlinx.coroutines.launch

@Composable
fun DiagnosticsScreen(
    viewModel: JarvisViewModel,
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    var contactCount by remember { mutableStateOf(0) }
    var calendarCount by remember { mutableStateOf(0) }
    var diagnosticLog by remember { mutableStateOf("All system modules initialized. Tap a test button to run verification.") }

    LaunchedEffect(Unit) {
        viewModel.refreshPermissionStatus()
        coroutineScope.launch {
            val contacts = viewModel.contactRepo.fetchAllPhoneContacts()
            contactCount = contacts.size
            val cal = viewModel.calendarRepo.getUpcomingEvents(10)
            calendarCount = cal.size
        }
    }

    val isWhatsAppInstalled = viewModel.appLauncher.isAppInstalled("com.whatsapp") || viewModel.appLauncher.isAppInstalled("com.whatsapp.w4b")
    val isInstagramInstalled = viewModel.appLauncher.isAppInstalled("com.instagram.android")
    val isYouTubeInstalled = viewModel.appLauncher.isAppInstalled("com.google.android.youtube")
    val isSpeechAvailable = viewModel.speechManager.isRecognitionAvailable()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF060A14))
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = JarvisCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "SYSTEM DIAGNOSTICS & VERIFICATION",
                        color = JarvisCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Hardware, Intent & Integration Pipeline",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Diagnostic Console Output
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("diagnostic_console_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF04070D)),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "DIAGNOSTIC TELEMETRY LOG",
                        color = JarvisCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = diagnosticLog,
                        color = Color(0xFFC7D7F0),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Core Subsystems Check
        item {
            Text(
                text = "1. VOICE & HARDWARE SUBSYSTEMS",
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B2C47))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiagnosticCheckRow("Speech Recognizer Service", isSpeechAvailable, if (isSpeechAvailable) "Active" else "Unavailable")
                    DiagnosticCheckRow("Microphone Permission", uiState.permissionStatus.hasMic, if (uiState.permissionStatus.hasMic) "Granted" else "Missing")
                    DiagnosticCheckRow("Contacts Provider", uiState.permissionStatus.hasContacts, "$contactCount contacts loaded")
                    DiagnosticCheckRow("Calendar Provider", uiState.permissionStatus.hasCalendar, "$calendarCount events available")
                    DiagnosticCheckRow("Phone Calling Permission", uiState.permissionStatus.hasPhoneCall, if (uiState.permissionStatus.hasPhoneCall) "Granted" else "Dialer Fallback")
                    DiagnosticCheckRow("Accessibility Automation", uiState.permissionStatus.hasAccessibility, if (uiState.permissionStatus.hasAccessibility) "Enabled" else "Disabled")
                }
            }
        }

        // Target Applications Check
        item {
            Text(
                text = "2. TARGET APPLICATIONS COMPATIBILITY",
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B2C47))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiagnosticCheckRow("WhatsApp (Messaging/Calls)", isWhatsAppInstalled, if (isWhatsAppInstalled) "Installed" else "Not installed (Store intent fallback)")
                    DiagnosticCheckRow("Instagram (App Launcher)", isInstagramInstalled, if (isInstagramInstalled) "Installed" else "Not installed")
                    DiagnosticCheckRow("YouTube (Video Search)", isYouTubeInstalled, if (isYouTubeInstalled) "Installed" else "Browser Web fallback")
                    DiagnosticCheckRow("Gemini Intent Engine", true, "Dual: Local NLP + Gemini 3.5 Flash")
                }
            }
        }

        // Live Diagnostic Test Actions
        item {
            Text(
                text = "3. INTERACTIVE VERIFICATION SUITE",
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Test TTS voice synthesis
                DiagnosticActionButton(
                    title = "Test Voice Synthesis (TTS)",
                    subtitle = "Plays Jarvis vocal feedback greeting",
                    onClick = {
                        diagnosticLog = "[TTS Test]: Synthesizing vocal greeting..."
                        viewModel.ttsManager.speak("Systems operational. Jarvis voice synthesis functioning smoothly.") {
                            diagnosticLog = "[TTS Test]: Voice synthesis completed successfully."
                        }
                    }
                )

                // Test YouTube Search Intent
                DiagnosticActionButton(
                    title = "Verify YouTube Search Intent",
                    subtitle = "Tests Intent.ACTION_SEARCH for \"Python tutorials\"",
                    onClick = {
                        diagnosticLog = "[YouTube Test]: Executing search intent for 'Python tutorials'..."
                        viewModel.processVoiceCommand("Open YouTube and search for Python tutorials")
                    }
                )

                // Test WhatsApp Intent
                DiagnosticActionButton(
                    title = "Verify WhatsApp Deep Link Intent",
                    subtitle = "Opens WhatsApp message prefill schema",
                    onClick = {
                        diagnosticLog = "[WhatsApp Test]: Triggering WhatsApp message prefill intent..."
                        viewModel.processVoiceCommand("Send a WhatsApp message to Rahul saying I will arrive in ten minutes")
                    }
                )

                // Test Hinglish parser
                DiagnosticActionButton(
                    title = "Verify Multilingual Hinglish Parser",
                    subtitle = "Tests: \"Papa ko call lagao\"",
                    onClick = {
                        diagnosticLog = "[Hinglish Test]: Parsing 'Papa ko call lagao'..."
                        viewModel.processVoiceCommand("Papa ko call lagao")
                    }
                )

                // Test Calendar retrieval
                DiagnosticActionButton(
                    title = "Verify Calendar Events Retrieval",
                    subtitle = "Queries CalendarContract.Events directly",
                    onClick = {
                        coroutineScope.launch {
                            val events = viewModel.calendarRepo.getUpcomingEvents(5)
                            val summary = viewModel.calendarRepo.formatEventsForSpeech(events)
                            diagnosticLog = "[Calendar Test]: Retrieved ${events.size} events.\n$summary"
                            viewModel.ttsManager.speak(summary)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DiagnosticCheckRow(
    title: String,
    isAvailable: Boolean,
    detail: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF091222))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isAvailable) JarvisLime.copy(alpha = 0.2f) else JarvisRed.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAvailable) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (isAvailable) JarvisLime else JarvisRed,
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Text(
            text = detail,
            color = if (isAvailable) JarvisCyan else Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DiagnosticActionButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("diag_action_${title.take(12).replace(" ", "_")}"),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF101B30),
            contentColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E355B)),
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = JarvisCyan
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = JarvisCyan,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
