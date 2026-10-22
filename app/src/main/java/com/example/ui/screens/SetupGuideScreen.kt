package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SettingsAccessibility
import androidx.compose.material.icons.filled.Smartphone
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisLime
import com.example.ui.theme.JarvisSurfaceCard

@Composable
fun SetupGuideScreen(
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = JarvisCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "JARVIS ARCHITECTURE & SETUP",
                        color = JarvisCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Phone Automation, Windows PC Bridge & Lockscreen",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Section 1: Windows PC Companion Server (FastAPI)
        item {
            GuideSectionCard(
                icon = Icons.Default.Computer,
                iconTint = JarvisCyan,
                title = "Windows PC Companion Server Setup",
                subtitle = "FastAPI Python Bridge for PC control",
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StepItem("1", "On your Windows PC, ensure Python 3.9+ is installed.")
                        StepItem("2", "Open PowerShell or Command Prompt, and install requirements: pip install fastapi uvicorn")
                        StepItem("3", "Run the provided companion server script: python scripts/jarvis_pc_server.py")
                        StepItem("4", "The script prints your PC local IPv4 (e.g. 192.168.1.5) and default auth token 'jarvis_secret_key'.")
                        StepItem("5", "In this app under 'PC Control' tab, enter your PC IP address and token, then tap 'Save & Ping PC'.")
                        StepItem("6", "Now say: 'Hi Jarvis, lock my PC', 'Play media on PC', or 'Open Chrome on PC'.")
                    }
                }
            )
        }

        // Section 2: Background Wake Word & Lockscreen Experience
        item {
            GuideSectionCard(
                icon = Icons.Default.Hearing,
                iconTint = JarvisLime,
                title = "Wake Word & Lock-Screen Integration",
                subtitle = "Hands-free \"Hi Jarvis\" & secure screen wake",
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BulletText("Wake Word Activation: Toggle 'Background Wake Word' on the 'PC Control' tab. Jarvis starts a foreground service with microphone listening.")
                        BulletText("Lock-Screen Experience: MainActivity declares showWhenLocked='true' and turnScreenOn='true'. When 'Hi Jarvis' is detected, your phone screen lights up and displays Jarvis HUD over the lockscreen.")
                        BulletText("Secure Authentication: Jarvis NEVER bypasses device lock security or steals PINs. Sensitive actions check Android Keyguard security before execution.")
                        BulletText("Startup on Boot: BootReceiver automatically re-starts Jarvis background listening upon device reboot if enabled.")
                    }
                }
            )
        }

        // Section 3: Android System Hardware Controls
        item {
            GuideSectionCard(
                icon = Icons.Default.Smartphone,
                iconTint = JarvisAmber,
                title = "System Toggles & Settings Panels",
                subtitle = "Flashlight, Volume, Files & Wi-Fi",
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BulletText("Flashlight / Torch: 'Turn on flashlight' / 'Turn off flashlight' uses CameraManager.setTorchMode.")
                        BulletText("Volume Control: 'Increase volume', 'Volume down', 'Mute audio' uses AudioManager.")
                        BulletText("Settings Panels: 'Open Wi-Fi settings', 'Open Bluetooth settings' launches direct Android Settings panels without breaking sandbox rules.")
                        BulletText("File Browser: 'Open files' launches the native Android document picker.")
                        BulletText("Web Search: 'Search the web for [topic]' launches Google Search.")
                    }
                }
            )
        }

        // Section 4: WhatsApp Automation Details
        item {
            GuideSectionCard(
                icon = Icons.Default.SettingsAccessibility,
                iconTint = JarvisCyan,
                title = "WhatsApp Automation & Accessibility Service",
                subtitle = "Direct deep links + optional auto-send",
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "WhatsApp Integration Pipeline:",
                            color = JarvisCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Messaging: Intent.ACTION_VIEW with https://api.whatsapp.com/send?phone=...&text=... pre-populates recipient and text.\n" +
                                    "• Video Calls: Intent.ACTION_VIEW querying the contact's WhatsApp mime type (vnd.android.cursor.item/vnd.com.whatsapp.video.call).\n" +
                                    "• Optional Auto-Send: Enabling JarvisAccessibilityService allows the app to automatically click 'Send' once WhatsApp opens.",
                            color = Color(0xFFC7D7F0),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = onOpenAccessibilitySettings,
                            modifier = Modifier.fillMaxWidth().testTag("open_accessibility_guide_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JarvisCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SettingsAccessibility,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Accessibility Settings", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            )
        }

        // Section 5: Voice Commands Reference
        item {
            GuideSectionCard(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                iconTint = JarvisCyan,
                title = "Voice Commands Reference (English & Hinglish)",
                subtitle = "Supported command phrasing recipes",
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CommandExample("Hi Jarvis, call Papa", "Wakes assistant, resolves Papa, initiates phone call.")
                        CommandExample("Lock my PC", "Locks your Windows PC workstation via FastAPI server.")
                        CommandExample("Play media on PC", "Toggles Spotify / YouTube media play/pause on PC.")
                        CommandExample("Turn on flashlight", "Toggles Android torch via CameraManager.")
                        CommandExample("Volume up / Aawaz badhao", "Increases Android media volume.")
                        CommandExample("Open Wi-Fi settings", "Opens Android Wi-Fi settings panel.")
                        CommandExample("Open files", "Opens Android file manager.")
                        CommandExample("Search the web for Python tutorials", "Performs web search.")
                        CommandExample("Send a WhatsApp message to Rahul saying I will arrive in ten minutes", "Pre-fills message to recipient with 5-second safety confirmation.")
                        CommandExample("Emergency Stop / Ruk jao", "Instantly kills all audio, speech, and pending actions.")
                    }
                }
            )
        }
    }
}

@Composable
private fun GuideSectionCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B2C47))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun StepItem(number: String, text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xFF1B2C47)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = number, color = JarvisCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = Color(0xFFC7D7F0),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BulletText(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("• ", color = JarvisCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(
            text = text,
            color = Color(0xFFC7D7F0),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CommandExample(command: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF091222))
            .padding(10.dp)
    ) {
        Text(
            text = "\"$command\"",
            color = JarvisCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            color = Color(0xFF8FA3C4),
            fontSize = 11.sp
        )
    }
}
