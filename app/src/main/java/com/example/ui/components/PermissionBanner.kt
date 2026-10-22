package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SettingsAccessibility
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisLime
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSurfaceCard

data class PermissionStatus(
    val hasMic: Boolean,
    val hasContacts: Boolean,
    val hasPhoneCall: Boolean,
    val hasCalendar: Boolean,
    val hasAccessibility: Boolean
)

@Composable
fun PermissionCard(
    status: PermissionStatus,
    onRequestPermissions: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allGranted = status.hasMic && status.hasContacts && status.hasPhoneCall && status.hasCalendar

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("permission_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E355B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Privacy & Permissions",
                    tint = if (allGranted) JarvisLime else JarvisGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Device Permissions & Privacy",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Private on-device execution. Never records in background.",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Permission status items
            PermissionItem(
                icon = Icons.Default.Mic,
                title = "Microphone",
                description = "For speech recognition and voice commands",
                isGranted = status.hasMic
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionItem(
                icon = Icons.Default.Contacts,
                title = "Contacts",
                description = "Resolve Papa, Rahul, and contacts for calls/WhatsApp",
                isGranted = status.hasContacts
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionItem(
                icon = Icons.Default.Call,
                title = "Phone Calling",
                description = "Direct dialing or phone dialer integration",
                isGranted = status.hasPhoneCall
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionItem(
                icon = Icons.Default.Event,
                title = "Calendar",
                description = "Read upcoming meetings and daily schedule",
                isGranted = status.hasCalendar
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionItem(
                icon = Icons.Default.SettingsAccessibility,
                title = "Accessibility Service",
                description = "Optional hands-free UI clicking (e.g. WhatsApp auto-send)",
                isGranted = status.hasAccessibility,
                isOptional = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!allGranted) {
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("grant_permissions_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JarvisCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Grant App Permissions", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                if (!status.hasAccessibility) {
                    Button(
                        onClick = onOpenAccessibilitySettings,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("enable_accessibility_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E355B),
                            contentColor = JarvisCyan
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Enable Automation", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    isOptional: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0A101E))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isGranted) JarvisLime else Color.Gray,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (isOptional) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(Optional)",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
            Text(
                text = description,
                color = Color(0xFF8898AA),
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isGranted) JarvisLime.copy(alpha = 0.2f) else JarvisRed.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Close,
                contentDescription = if (isGranted) "Granted" else "Missing",
                tint = if (isGranted) JarvisLime else JarvisRed,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
