package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommandType
import com.example.data.model.ParsedIntent
import com.example.data.model.PcAction
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSurfaceCard

@Composable
fun ActionConfirmationCard(
    intent: ParsedIntent,
    remainingSeconds: Int,
    totalSeconds: Int = 5,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)

    val (actionTitle, actionIcon, actionColor) = when {
        intent.commandType == CommandType.PC_COMMAND && intent.pcAction == PcAction.SHUTDOWN_PC -> Triple(
            "Shutdown Windows PC",
            Icons.Default.PowerSettingsNew,
            JarvisRed
        )
        intent.commandType == CommandType.WHATSAPP_MESSAGE -> Triple(
            "Send WhatsApp Message",
            Icons.AutoMirrored.Filled.Message,
            JarvisCyan
        )
        intent.commandType == CommandType.CALL_PHONE -> Triple(
            "Place Phone Call",
            Icons.Default.Call,
            JarvisGold
        )
        intent.commandType == CommandType.WHATSAPP_VIDEO_CALL -> Triple(
            "WhatsApp Video Call",
            Icons.Default.Videocam,
            JarvisCyan
        )
        intent.commandType == CommandType.WHATSAPP_VOICE_CALL -> Triple(
            "WhatsApp Voice Call",
            Icons.Default.Call,
            JarvisCyan
        )
        else -> Triple(
            "Execute Action",
            Icons.Default.Security,
            JarvisAmber
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, actionColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .testTag("action_confirmation_card"),
        color = JarvisSurfaceCard
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with Security / Confirmation Notice
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(actionColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = actionTitle,
                        tint = actionColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = actionTitle,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Safety Check: Auto-executes in ${remainingSeconds}s",
                        color = JarvisAmber,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Details Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF09101F))
                    .border(1.dp, Color(0xFF1B2A4A), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column {
                    if (!intent.contactName.isNullOrEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Recipient: ",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = intent.contactName + (intent.resolvedPhoneNumber?.let { " ($it)" } ?: ""),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (!intent.messageBody.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Message: ",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "\"${intent.messageBody}\"",
                            color = JarvisCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    if (!intent.searchQuery.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Search Query: \"${intent.searchQuery}\"",
                            color = JarvisCyan,
                            fontSize = 13.sp
                        )
                    }

                    if (intent.commandType == CommandType.PC_COMMAND) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Target Host: Windows PC Workstation",
                            color = JarvisCyan,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Countdown Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = actionColor,
                trackColor = Color(0xFF1B2A4A)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("cancel_action_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = JarvisRed
                    ),
                    border = BorderStroke(1.dp, JarvisRed.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("confirm_action_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = actionColor,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "Confirm",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirm Now", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
