package com.example.mycomposeapp.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SelfImprovement

private val White = Color(0xFFFFFFFF)
private val Black = Color(0xFF000000)
private val Gray = Color(0xFF888888)

@Composable
fun DeepfakeAIScreen(
    onBack: () -> Unit,
    onCardClick: () -> Unit,
    onGenerationSuccess: () -> Unit = {}
) {
    var selectedPreset by remember { mutableStateOf<String?>(null) }
    var selectedOutput by remember { mutableStateOf<String?>(null) }
    var promptText by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // A. Main Container Title
            Text(
                text = "AI Face Fusion & Deepfake AI Frame",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = White,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // B. Face Upload Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, White, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Face Upload",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = White,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Left: Source Face
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, White, RoundedCornerShape(8.dp))
                                .clickable(onClick = onCardClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Source Face",
                                    tint = White,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "SOURCE FACE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Right: Target Frame
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, White, RoundedCornerShape(8.dp))
                                .clickable(onClick = onCardClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Target Frame",
                                    tint = White,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "TARGET FRAME",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // C. Quick Motion Presets
            Text(
                text = "Quick Motion Presets",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = White,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, White, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PresetButton(
                            text = "Head Rotation",
                            isSelected = selectedPreset == "Head Rotation",
                            onClick = { selectedPreset = "Head Rotation" }
                        )
                        PresetButton(
                            text = "Natural Liveness",
                            isSelected = selectedPreset == "Natural Liveness",
                            onClick = { selectedPreset = "Natural Liveness" }
                        )
                        PresetButton(
                            text = "Blink & Nod",
                            isSelected = selectedPreset == "Blink & Nod",
                            onClick = { selectedPreset = "Blink & Nod" }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // D. Motion Generation Prompt
            Text(
                text = "Motion Generation Prompt",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = White,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, White, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    BasicTextField(
                        value = promptText,
                        onValueChange = {
                            if (it.length <= 500) {
                                promptText = it
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .border(1.dp, White, RoundedCornerShape(8.dp)),
                        textStyle = TextStyle(
                            color = White,
                            fontSize = 12.sp
                        ),
                        cursorBrush = SolidColor(White),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (promptText.isEmpty()) {
                                    Text(
                                        text = "Describe your custom motion instructions or select a preset above...",
                                        fontSize = 12.sp,
                                        color = Gray
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${promptText.length}/500",
                        fontSize = 10.sp,
                        color = Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // E. Output Selection
            Text(
                text = "Output Type",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = White,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutputButton(
                    text = "Video Output",
                    icon = Icons.Default.Videocam,
                    isSelected = selectedOutput == "Video",
                    onClick = { selectedOutput = "Video" }
                )
                OutputButton(
                    text = "Image Output",
                    icon = Icons.Default.Image,
                    isSelected = selectedOutput == "Image",
                    onClick = { selectedOutput = "Image" }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // F. Generate Button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, White, RoundedCornerShape(12.dp))
                    .clickable(onClick = {
                        onCardClick()
                        onGenerationSuccess()
                    }),
                colors = CardDefaults.cardColors(containerColor = Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = "Generate",
                        tint = White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generate AI Motion Video",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PresetButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(80.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, White, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF333333) else Black
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        )
    }
}

@Composable
private fun OutputButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, White, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF333333) else Black
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = White,
                textAlign = TextAlign.Center
            )
        }
    }
}
