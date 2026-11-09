package com.example.mycomposeapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VideoCameraFront

private val White = Color(0xFFFFFFFF)
private val Black = Color(0xFF000000)
private val DarkGray = Color(0xFF12101F)

@Composable
fun HomeScreen(
    onVirtualCameraClick: () -> Unit = {},
    onDeepfakeAIClick: () -> Unit = {},
    onOBSConnectClick: () -> Unit = {},
    onCreateDocClick: () -> Unit = {},
    onTelegramClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Navigation (top-aligned) ──
            TopNavigation(onNotificationClick = onNotificationClick)

            Spacer(modifier = Modifier.height(16.dp))

            // ── Feature Cards (centered in remaining space) ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                FeatureCards(
                    onVirtualCameraClick = onVirtualCameraClick,
                    onDeepfakeAIClick = onDeepfakeAIClick,
                    onOBSConnectClick = onOBSConnectClick,
                    onCreateDocClick = onCreateDocClick
                )
            }

            // ── Bottom Shortcuts ──
            BottomShortcuts(onTelegramClick = onTelegramClick)

            Spacer(modifier = Modifier.height(8.dp))

            // ── Footer ──
            Footer()
        }
    }
}

// ============================================================================
// TOP NAVIGATION
// ============================================================================

@Composable
private fun TopNavigation(
    onNotificationClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Notification Bell
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onNotificationClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ============================================================================
// FEATURE CARDS (2x2 Grid)
// ============================================================================

data class FeatureCardData(
    val title: String,
    val icon: ImageVector
)

@Composable
private fun FeatureCards(
    onVirtualCameraClick: () -> Unit,
    onDeepfakeAIClick: () -> Unit,
    onOBSConnectClick: () -> Unit,
    onCreateDocClick: () -> Unit
) {
    val cards = listOf(
        FeatureCardData(
            title = "VIRTUAL CAMERA",
            icon = Icons.Default.VideoCameraFront
        ),
        FeatureCardData(
            title = "deepfake AI",
            icon = Icons.Default.Face
        ),
        FeatureCardData(
            title = "OBS CONNECT",
            icon = Icons.AutoMirrored.Default.ScreenShare
        ),
        FeatureCardData(
            title = "CREATE DOC",
            icon = Icons.Default.Description
        )
    )

    // 2 rows of 2 cards each
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FeatureCard(
                card = cards[0],
                onClick = onVirtualCameraClick
            )
            FeatureCard(
                card = cards[1],
                onClick = onDeepfakeAIClick
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FeatureCard(
                card = cards[2],
                onClick = onOBSConnectClick
            )
            FeatureCard(
                card = cards[3],
                onClick = onCreateDocClick
            )
        }
    }
}

@Composable
private fun FeatureCard(
    card: FeatureCardData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(110.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(1.dp, White, RoundedCornerShape(12.dp))
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Black
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Icon(
                imageVector = card.icon,
                contentDescription = card.title,
                tint = White,
                modifier = Modifier
                    .size(36.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = card.title.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = White,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ============================================================================
// BOTTOM SHORTCUTS
// ============================================================================

@Composable
private fun BottomShortcuts(onTelegramClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .clickable(onTelegramClick)
                .shadow(8.dp, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Button background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Black, CircleShape)
                    .border(1.dp, White, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.Send,
                    contentDescription = "Telegram",
                    tint = White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ============================================================================
// FOOTER
// ============================================================================

@Composable
private fun Footer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // White status indicator
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(White)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = "powered by Klassiq KYC",
            fontSize = 10.sp,
            color = White,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
