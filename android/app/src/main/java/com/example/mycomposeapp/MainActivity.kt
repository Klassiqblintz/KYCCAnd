package com.example.mycomposeapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mycomposeapp.ui.DeepfakeAIScreen
import com.example.mycomposeapp.ui.HomeScreen
import com.example.mycomposeapp.ui.NotificationScreen
import com.example.mycomposeapp.ui.OBSConnectScreen
import com.example.mycomposeapp.ui.CreateDocScreen
import com.example.mycomposeapp.ui.VirtualCameraScreen
import com.example.mycomposeapp.ui.NotificationItem
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var showComingSoon by remember { mutableStateOf(false) }

            if (showComingSoon) {
                AlertDialog(
                    onDismissRequest = { showComingSoon = false },
                    title = { Text("Coming Soon") },
                    text = { Text("This feature will available in phase 3.") },
                    confirmButton = {
                        TextButton(onClick = { showComingSoon = false }) {
                            Text("OK")
                        }
                    }
                )
            }

            // ── Notification state (shared across all screens) ──
            val notifications = remember { mutableStateListOf<NotificationItem>() }

            fun addNotification(type: String, title: String, message: String, dotColor: androidx.compose.ui.graphics.Color) {
                val format = SimpleDateFormat("HH:mm:ss • MM/dd/yyyy", Locale.US)
                val timestamp = format.format(Date())
                notifications.add(0, NotificationItem(type, title, message, timestamp, dotColor))
            }

            val onAddNotification = { type: String, title: String, message: String, dotColor: androidx.compose.ui.graphics.Color ->
                addNotification(type, title, message, dotColor)
            }

            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = "home"
            ) {
                composable("home") {
                    HomeScreen(
                        onVirtualCameraClick = {
                            navController.navigate("virtual_camera")
                        },
                        onDeepfakeAIClick = {
                            navController.navigate("deepfake_ai")
                        },
                        onOBSConnectClick = {
                            navController.navigate("obs_connect")
                        },
                        onCreateDocClick = {
                            navController.navigate("create_doc")
                        },
                        onTelegramClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/+jedMLMUh_-FhN2Q0"))
                            startActivity(intent)
                        },
                        onNotificationClick = {
                            navController.navigate("notifications")
                        }
                    )
                }
                composable("notifications") {
                    NotificationScreen(
                        notifications = notifications.toList(),
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("virtual_camera") {
                    VirtualCameraScreen(
                        onBack = { navController.popBackStack() },
                        onCardClick = { showComingSoon = true },
                        onBypassSuccess = {
                            addNotification(
                                "bypass",
                                "Target App Bypassed",
                                "Virtual camera is now feeding the target app successfully.",
                                androidx.compose.ui.graphics.Color(0xFF00E676)
                            )
                        }
                    )
                }
                composable("deepfake_ai") {
                    DeepfakeAIScreen(
                        onBack = { navController.popBackStack() },
                        onCardClick = { showComingSoon = true },
                        onGenerationSuccess = {
                            addNotification(
                                "deepfake",
                                "Deepfake AI Generated",
                                "AI face fusion video has been generated successfully.",
                                androidx.compose.ui.graphics.Color(0xFFD500F9)
                            )
                        }
                    )
                }
                composable("create_doc") {
                    CreateDocScreen(
                        onBack = { navController.popBackStack() },
                        onDocumentGenerated = { bitmap ->
                            addNotification(
                                "document",
                                "Document Created",
                                "Your document has been generated successfully.",
                                androidx.compose.ui.graphics.Color(0xFF448AFF)
                            )
                        }
                    )
                }
                composable("obs_connect") {
                    OBSConnectScreen(
                        onBack = { navController.popBackStack() },
                        onCardClick = { showComingSoon = true },
                        onConnected = {
                            addNotification(
                                "obs",
                                "OBS Connected",
                                "Successfully connected to OBS — bridge is active.",
                                androidx.compose.ui.graphics.Color(0xFFFF9100)
                            )
                        }
                    )
                }
            }
        }
    }
}
