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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.MenuBook
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
                    imageVector = Icons.Default.MenuBook,
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
                        text = "Windows, Android Studio & Real Device Guide",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Section 1: Windows & Android Studio Setup
        item {
            GuideSectionCard(
                icon = Icons.Default.Code,
                iconTint = JarvisCyan,
                title = "Windows & Android Studio Setup",
                subtitle = "Building & running on local machine",
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StepItem("1", "Clone or extract the project ZIP to a local folder (e.g. C:\\Projects\\JarvisAssistant).")
                        StepItem("2", "Open Android Studio (Ladybug / Koala / Electric Eel or newer). Select 'Open' and choose the project root.")
                        StepItem("3", "Ensure JDK 17 or JDK 21 is selected under Settings -> Build, Execution, Deployment -> Build Tools -> Gradle.")
                        StepItem("4", "To build from Windows Command Prompt / PowerShell, run: gradle assembleDebug (or use Android Studio Run 'app').")
                        StepItem("5", "To use Gemini 3.5 Flash for online intent parsing, set GEMINI_API_KEY in .env. When offline, Jarvis automatically uses its embedded on-device regex/NLP engine.")
                    }
                }
            )
        }

        // Section 2: Real Android Device Testing & WhatsApp Automation
        item {
            GuideSectionCard(
                icon = Icons.Default.Smartphone,
                iconTint = JarvisLime,
                title = "Real Device Testing & WhatsApp Automation",
                subtitle = "How real Android system intents work",
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "WhatsApp Deep Link & Call Pipeline:",
                            color = JarvisCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Meta does not provide a public background messaging API for WhatsApp without enterprise servers. Jarvis uses supported Android Intents:\n" +
                                    "• Messaging: Intent.ACTION_VIEW with https://api.whatsapp.com/send?phone=...&text=... pre-populates the recipient and message.\n" +
                                    "• Video/Voice Calls: Intent.ACTION_VIEW querying the contact's WhatsApp mime type (vnd.android.cursor.item/vnd.com.whatsapp.video.call).\n" +
                                    "• Hands-Free Automation: Enabling JarvisAccessibilityService allows the app to automatically click 'Send' once WhatsApp opens.",
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

        // Section 3: Privacy & Consequential Action Safeguards
        item {
            GuideSectionCard(
                icon = Icons.Default.PrivacyTip,
                iconTint = JarvisAmber,
                title = "Permissions, Privacy & Emergency Stop",
                subtitle = "Zero background eavesdropping & safety gates",
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BulletText("No Background Recording: Jarvis only activates the microphone when you tap the Arc Reactor or trigger voice mode.")
                        BulletText("5-Second Safety Safeguard: Before placing phone calls or sending messages, Jarvis displays a safety card with a countdown timer, allowing you to abort.")
                        BulletText("Emergency Stop Command: Say 'Stop', 'Ruk jao', 'Cancel', or tap the red HALT button at any time to instantly stop speech, abort pending actions, and kill listeners.")
                        BulletText("Local Persistence: Command logs are stored locally on your device in a private Room database.")
                    }
                }
            )
        }

        // Section 4: Multilingual Voice Command Cheatsheet
        item {
            GuideSectionCard(
                icon = Icons.Default.MenuBook,
                iconTint = JarvisCyan,
                title = "Voice Commands Reference (English & Hinglish)",
                subtitle = "Supported command phrasing recipes",
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CommandExample("Open WhatsApp", "Opens WhatsApp application directly.")
                        CommandExample("Open Instagram", "Opens Instagram application directly.")
                        CommandExample("Call Papa", "Resolves contact 'Papa' / 'Dad' and initiates dialer/call.")
                        CommandExample("Papa ko call lagao", "Hinglish calling command.")
                        CommandExample("Open WhatsApp and video call Papa", "Triggers direct WhatsApp video call.")
                        CommandExample("Send a WhatsApp message to Rahul saying I will arrive in ten minutes", "Pre-fills message to recipient with 5-second safety confirmation.")
                        CommandExample("Rahul ko WhatsApp message bhejo ki 10 minute me aa raha hu", "Hinglish WhatsApp message with prefilled text.")
                        CommandExample("Open YouTube and search for Python tutorials", "Launches YouTube search intent.")
                        CommandExample("Read my upcoming calendar events", "Fetches upcoming schedule from device calendar and speaks it aloud.")
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
