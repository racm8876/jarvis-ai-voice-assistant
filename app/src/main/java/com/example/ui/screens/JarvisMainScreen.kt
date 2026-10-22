package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ResolvedContact
import com.example.ui.AssistantStatus
import com.example.ui.JarvisViewModel
import com.example.ui.components.ActionConfirmationCard
import com.example.ui.components.ArcReactorVisualizer
import com.example.ui.components.PermissionCard
import com.example.ui.components.VisualizerState
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisLime
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSurfaceCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JarvisMainScreen(
    viewModel: JarvisViewModel,
    onRequestPermissions: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val rmsLevel by viewModel.rmsLevel.collectAsState()

    val quickCommands = listOf(
        "Open WhatsApp",
        "Call Papa",
        "Send a WhatsApp message to Rahul saying I will arrive in ten minutes",
        "Lock my PC",
        "Turn on flashlight",
        "Open YouTube and search for Python tutorials",
        "Read my upcoming calendar events",
        "Search the web for Gemini AI",
        "Open files",
        "Increase volume",
        "Papa ko call lagao",
        "Rahul ko WhatsApp message bhejo ki I will arrive in 10 minutes"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF060A14))
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top HUD Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (uiState.status) {
                                    AssistantStatus.LISTENING -> JarvisCyan
                                    AssistantStatus.PROCESSING -> JarvisAmber
                                    AssistantStatus.EMERGENCY_STOP -> JarvisRed
                                    else -> JarvisLime
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "JARVIS SYSTEM // ONLINE",
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Mute / Unmute TTS
                    IconButton(
                        onClick = { viewModel.toggleMute() },
                        modifier = Modifier.testTag("mute_tts_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isTtsMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Toggle Speech Feedback",
                            tint = if (uiState.isTtsMuted) Color.Gray else JarvisCyan
                        )
                    }

                    // Emergency Stop Button
                    Button(
                        onClick = { viewModel.emergencyStop() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("emergency_stop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Emergency Stop",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("HALT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Central Arc Reactor Visualizer & Core Control
        item {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ArcReactorVisualizer(
                    state = uiState.visualizerState,
                    rmsLevel = rmsLevel,
                    size = 230.dp,
                    onClick = { viewModel.toggleListening() }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Status Badge
            Surface(
                color = when (uiState.status) {
                    AssistantStatus.LISTENING -> JarvisCyan.copy(alpha = 0.15f)
                    AssistantStatus.PROCESSING -> JarvisAmber.copy(alpha = 0.15f)
                    AssistantStatus.EMERGENCY_STOP -> JarvisRed.copy(alpha = 0.15f)
                    else -> Color(0xFF101B30)
                },
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    when (uiState.status) {
                        AssistantStatus.LISTENING -> JarvisCyan
                        AssistantStatus.PROCESSING -> JarvisAmber
                        AssistantStatus.EMERGENCY_STOP -> JarvisRed
                        else -> Color(0xFF1E355B)
                    }
                )
            ) {
                Text(
                    text = when (uiState.status) {
                        AssistantStatus.LISTENING -> "● LISTENING TO VOICE..."
                        AssistantStatus.PROCESSING -> "⟳ PARSING INTENT..."
                        AssistantStatus.CONFIRMATION_PENDING -> "⚠ SAFETY CONFIRMATION"
                        AssistantStatus.EXECUTING -> "▶ EXECUTING INTENT..."
                        AssistantStatus.SPEAKING -> "▶ SPEAKING RESPONSE..."
                        AssistantStatus.EMERGENCY_STOP -> "■ EMERGENCY HALTED"
                        AssistantStatus.ERROR -> "⚠ SYSTEM WARNING"
                        AssistantStatus.IDLE -> "TAP CORE TO SPEAK"
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (uiState.status) {
                        AssistantStatus.LISTENING -> JarvisCyan
                        AssistantStatus.PROCESSING -> JarvisAmber
                        AssistantStatus.EMERGENCY_STOP -> JarvisRed
                        else -> Color(0xFFB0C4DE)
                    },
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        // 3. User Speech Transcript Card
        item {
            AnimatedVisibility(
                visible = uiState.transcript.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Text(
                        text = "VOICE INPUT",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("transcript_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E355B))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = JarvisCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "\"${uiState.transcript}\"",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 4. Jarvis Speech Reply Box
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                modifier = Modifier.fillMaxWidth().testTag("assistant_reply_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF091222)),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "JARVIS FEEDBACK",
                            color = JarvisCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        if (uiState.status == AssistantStatus.SPEAKING) {
                            Text(
                                text = "VOCALIZING",
                                color = JarvisGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.assistantReply,
                        color = Color(0xFFE3EDFC),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // 5. Consequential Safety Confirmation Card (Section 5 requirement)
        item {
            AnimatedVisibility(
                visible = uiState.status == AssistantStatus.CONFIRMATION_PENDING && uiState.pendingAction != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.pendingAction?.let { pendingIntent ->
                    Spacer(modifier = Modifier.height(14.dp))
                    ActionConfirmationCard(
                        intent = pendingIntent,
                        remainingSeconds = uiState.countdownRemaining,
                        onConfirm = { viewModel.confirmPendingAction() },
                        onCancel = { viewModel.cancelPendingAction() }
                    )
                }
            }
        }

        // 6. Clarification Resolution Card (for ambiguous contact names)
        item {
            AnimatedVisibility(
                visible = uiState.clarificationContacts.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(JarvisSurfaceCard)
                        .border(1.dp, JarvisAmber, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = uiState.clarificationMessage ?: "Which contact did you mean?",
                        color = JarvisAmber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.clarificationContacts.forEach { contact ->
                        ClarificationContactItem(
                            contact = contact,
                            onClick = { viewModel.selectClarificationContact(contact) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // 7. Quick Voice Command Testing Chips
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SAMPLE VOICE COMMANDS (TAP TO EXECUTE)",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickCommands.forEach { cmd ->
                        SuggestionChip(
                            onClick = {
                                viewModel.processVoiceCommand(cmd)
                            },
                            label = {
                                Text(
                                    text = cmd,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = Color(0xFF101B30)
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = Color(0xFF1E355B)
                            ),
                            modifier = Modifier.testTag("quick_command_chip_${cmd.take(15).replace(" ", "_")}")
                        )
                    }
                }
            }
        }

        // 8. Device Permissions & Privacy Card
        item {
            Spacer(modifier = Modifier.height(20.dp))
            PermissionCard(
                status = uiState.permissionStatus,
                onRequestPermissions = onRequestPermissions,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings
            )
        }
    }
}

@Composable
private fun ClarificationContactItem(
    contact: ResolvedContact,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF091222))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(JarvisCyan.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = JarvisCyan,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = contact.phoneNumber,
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
        Text(
            text = "Select",
            color = JarvisCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
