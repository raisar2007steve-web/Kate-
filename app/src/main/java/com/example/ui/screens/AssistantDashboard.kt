package com.example.ui.screens

import android.widget.Toast
import kotlinx.coroutines.delay
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ChatMessage
import com.example.data.Note
import com.example.data.Task
import com.example.viewmodel.AssistantViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin

enum class IosLaunchApp {
    SIRI_CHAT, WEATHER_NEWS, REMINDERS, NOTES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantDashboard(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeApp by remember { mutableStateOf(IosLaunchApp.SIRI_CHAT) }

    // Collect TTS and widgets statuses
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val isVoiceMuted by viewModel.isVoiceMuted.collectAsStateWithLifecycle()
    
    // Beautiful gradient representing iOS standard wallpaper (Lavender aurora meets deep sunset)
    val iosWallpaperBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0F172A), // Midnight Void
            Color(0xFF2E1065), // Cosmic Deep Violet
            Color(0xFF4C1D95), // Vibrant Royal Iris
            Color(0xFF5B21B6), // Purple
            Color(0xFF1E3A8A)  // Cool Deep Sea Blue
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(iosWallpaperBrush)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // 1. Sleek Apple-style Status Bar
            IosStatusBar()

            // 2. Main Active Application Window (Siri Chat, Weather, Reminders, Notes)
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = activeApp,
                    transitionSpec = {
                        slideInVertically(
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            initialOffsetY = { it }
                        ) + fadeIn() togetherWith
                        slideOutVertically(
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            targetOffsetY = { -it }
                        ) + fadeOut()
                    },
                    label = "IosAppTransitionPortal"
                ) { app ->
                    when (app) {
                        IosLaunchApp.SIRI_CHAT -> SiriChatAppView(viewModel)
                        IosLaunchApp.WEATHER_NEWS -> WeatherNewsAppView(viewModel)
                        IosLaunchApp.REMINDERS -> IosRemindersAppView(viewModel)
                        IosLaunchApp.NOTES -> IosNotesAppView(viewModel)
                    }
                }
            }

            // 3. Apple iOS Bottom Translucent App Dock
            IosBottomAppDock(
                activeApp = activeApp,
                onAppLaunch = { activeApp = it },
                isSpeaking = isSpeaking,
                isMuted = isVoiceMuted
            )
        }
    }
}

@Composable
fun IosStatusBar() {
    // Top system status indicator row representing Apple bar
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var currentTimeStr by remember { mutableStateOf(timeFormat.format(Date())) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeStr = timeFormat.format(Date())
            delay(10000)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "KATE.OS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Icon(
                imageVector = Icons.Default.SignalCellular4Bar,
                contentDescription = "iOS Network Active",
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "5G",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Encrypted Local Standalone Channel",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = currentTimeStr,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.SansSerif
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = "Wi-Fi Connected",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            
            // Battery capsule
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "94%",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(3.dp))
                Box(
                    modifier = Modifier
                        .size(width = 11.dp, height = 6.dp)
                        .background(Color(0xFF34C759), RoundedCornerShape(1f))
                )
            }
        }
    }
}

@Composable
fun IosBottomAppDock(
    activeApp: IosLaunchApp,
    onAppLaunch: (IosLaunchApp) -> Unit,
    isSpeaking: Boolean,
    isMuted: Boolean
) {
    // Beautiful glassmorphic bottom tray holding Apple iOS dock icons
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp, top = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .shadow(16.dp, shape = RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White.copy(alpha = 0.12f)) // Apple classic translucent bar
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon 1: Siri Chat
            DockAppIcon(
                label = "Siri Chat",
                iconVector = Icons.Default.Cyclone,
                accentColor = Color(0xFF00F0FF),
                isSelected = activeApp == IosLaunchApp.SIRI_CHAT,
                isPulse = isSpeaking,
                onClick = { onAppLaunch(IosLaunchApp.SIRI_CHAT) }
            )

            // App Icon 2: Weather & News (worldmonitor.app)
            DockAppIcon(
                label = "Weather",
                iconVector = Icons.Default.WbSunny,
                accentColor = Color(0xFFFF9500),
                isSelected = activeApp == IosLaunchApp.WEATHER_NEWS,
                onClick = { onAppLaunch(IosLaunchApp.WEATHER_NEWS) }
            )

            // App Icon 3: Reminders (Checklists)
            DockAppIcon(
                label = "Reminders",
                iconVector = Icons.Default.ListAlt,
                accentColor = Color(0xFF34C759),
                isSelected = activeApp == IosLaunchApp.REMINDERS,
                onClick = { onAppLaunch(IosLaunchApp.REMINDERS) }
            )

            // App Icon 4: Notes (Encrypted matrices)
            DockAppIcon(
                label = "Notes",
                iconVector = Icons.Default.StickyNote2,
                accentColor = Color(0xFFFFCC00),
                isSelected = activeApp == IosLaunchApp.NOTES,
                onClick = { onAppLaunch(IosLaunchApp.NOTES) }
            )
        }
    }
}

@Composable
fun DockAppIcon(
    label: String,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    isSelected: Boolean,
    isPulse: Boolean = false,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "DockPulse")
    val pulseValue by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val scaleFactor = if (isPulse) pulseValue else if (isSelected) 1.05f else 1.0f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .shadow(4.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(accentColor, accentColor.copy(alpha = 0.7f))))
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) accentColor else Color.White.copy(alpha = 0.8f)
        )
    }
}

// ==========================================
// SCREEN A: Siri-Style Voice Assistant App
// ==========================================
@Composable
fun SiriChatAppView(viewModel: AssistantViewModel) {
    val context = LocalContext.current
    val messages by viewModel.messagesState.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val chatInput by viewModel.chatInput.collectAsStateWithLifecycle()
    
    val voicePitch by viewModel.voicePitch.collectAsStateWithLifecycle()
    val voiceSpeed by viewModel.voiceSpeed.collectAsStateWithLifecycle()
    val isMuted by viewModel.isVoiceMuted.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()

    val chatScrollState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(messages.size, isAiLoading) {
        if (messages.isNotEmpty()) {
            chatScrollState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Siri Ambient Waveform widget
        SiriVoiceWidget(
            isSpeaking = isSpeaking,
            isMuted = isMuted,
            voicePitch = voicePitch,
            voiceSpeed = voiceSpeed,
            onPitchChange = { viewModel.updateVoicePitch(it) },
            onSpeedChange = { viewModel.updateVoiceSpeed(it) },
            onToggleMute = { viewModel.toggleVoiceMuted() },
            onStopVoice = { viewModel.stopSpeaking() }
        )

        // Clear button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "CLEAR DIALOGUE",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .clickable { 
                        viewModel.clearChat()
                        Toast.makeText(context, "Siri memory sync complete.", Toast.LENGTH_SHORT).show()
                    }
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Dialogue Log
        LazyColumn(
            state = chatScrollState,
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .shadow(8.dp, RoundedCornerShape(50))
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFF00F0FF), Color(0xFFA855F7), Color.Transparent)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cyclone,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "How can I help you, Steve?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "My standalone operating voice is optimized with high-harmonics for a sweet, comforting, and humanly responsive dialogue.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Suggest actions
                        val fastTriggers = listOf(
                            "Check Cupertino atmospheric weather",
                            "Read latest world headlines news",
                            "Draft my automation priorities plan"
                        )
                        fastTriggers.forEach { trigger ->
                            Card(
                                onClick = { viewModel.sendMessage(trigger) },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.08f)
                                ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFF00F0FF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = trigger,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                items(messages) { message ->
                    IosMessageBubble(
                        message = message,
                        onSpeakText = { text -> viewModel.speak(text) },
                        onSaveAsNote = { text ->
                            viewModel.addNote("LOG EXTRACT (" + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) + ")", text)
                            Toast.makeText(context, "Saved to Notes successfully!", Toast.LENGTH_SHORT).show()
                        },
                        onSaveAsReminder = { text ->
                            viewModel.addTask("Review Extract: " + text.take(20) + "...", text, "MEDIUM")
                            Toast.makeText(context, "Queued task successfully!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            if (isAiLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF00F0FF)
                        )
                        Text(
                            text = "Siri connection thinking...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Apple terminal interactive command bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = chatInput,
                onValueChange = { viewModel.updateChatInput(it) },
                placeholder = { Text("Command Siri assist...", color = Color.White.copy(alpha = 0.5f)) },
                modifier = Modifier
                    .weight(1.0f)
                    .clip(RoundedCornerShape(20.dp))
                    .testTag("chat_input_field"),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color.White.copy(alpha = 0.12f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.08f)
                ),
                maxLines = 2,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (chatInput.trim().isNotEmpty() && !isAiLoading) {
                        viewModel.sendMessage(chatInput)
                        keyboardController?.hide()
                    }
                })
            )

            Spacer(modifier = Modifier.width(8.dp))

            FloatingActionButton(
                onClick = {
                    if (chatInput.trim().isNotEmpty() && !isAiLoading) {
                        viewModel.sendMessage(chatInput)
                        keyboardController?.hide()
                    }
                },
                containerColor = Color(0xFF00F0FF),
                contentColor = Color.Black,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .size(46.dp)
                    .testTag("send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Submit Command",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SiriVoiceWidget(
    isSpeaking: Boolean,
    isMuted: Boolean,
    voicePitch: Float,
    voiceSpeed: Float,
    onPitchChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onStopVoice: () -> Unit
) {
    // Beautiful widget displaying a dynamic glowing waveform representation
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isSpeaking) Color(0xFF00FF66) else Color(0xFF00F0FF))
                    )
                    Text(
                        text = "Siri Voice Synthesizer".uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }

                // Volume helper
                IconButton(
                    onClick = onToggleMute,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = "Mute Voice Trigger",
                        tint = if (isMuted) Color.Red else Color(0xFF00F0FF),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Waveform core canvas
            SiriCustomWaveform(isActive = isSpeaking)

            Spacer(modifier = Modifier.height(14.dp))

            // Sliding controllers for speech parameters & high adjustments
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1.0f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vocal Pitch (Sweetness)", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                        Text("${"%.2f".format(voicePitch)}x", fontSize = 10.sp, color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = voicePitch,
                        onValueChange = onPitchChange,
                        valueRange = 0.8f..1.8f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00F0FF),
                            activeTrackColor = Color(0xFF00F0FF)
                        )
                    )
                }

                Column(modifier = Modifier.weight(1.0f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cadence rate (Warmth)", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                        Text("${"%.2f".format(voiceSpeed)}x", fontSize = 10.sp, color = Color(0xFFA855F7), fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = voiceSpeed,
                        onValueChange = onSpeedChange,
                        valueRange = 0.6f..1.4f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFA855F7),
                            activeTrackColor = Color(0xFFA855F7)
                        )
                    )
                }
            }

            if (isSpeaking) {
                Button(
                    onClick = onStopVoice,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f), contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.Red),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                ) {
                    Text("Deactivate Active speech stream", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SiriCustomWaveform(isActive: Boolean) {
    // Elegant real-time mathematical sine-wave animation that scales correctly
    val infiniteTransition = rememberInfiniteTransition(label = "SiriWaveform")
    val phaseOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * java.lang.Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val maxAmplitude = if (isActive) height * 0.38f else height * 0.08f

        // Draw multiple overlapping semi-transparent sine wave tracks with sliding phases representing voice components
        val waves = listOf(
            Triple(0.006f, Color(0xFF00F0FF).copy(alpha = 0.7f), 1f), // Primary Blue wave
            Triple(0.012f, Color(0xFFA855F7).copy(alpha = 0.5f), 1.5f), // Violet octave
            Triple(0.009f, Color(0xFFFF007F).copy(alpha = 0.4f), 0.8f) // Magenta depth
        )

        waves.forEach { (frequency, color, phaseSpeed) ->
            val path = Path()
            path.moveTo(0f, centerY)
            for (x in 0..width.toInt() step 4) {
                // Sine wave formula: y = amplitude * sin(freq * x + phase) + centerY
                val currentPhase = phaseOffset * phaseSpeed
                val y = (maxAmplitude * sin(frequency * x + currentPhase)) + centerY
                path.lineTo(x.toFloat(), y)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
fun IosMessageBubble(
    message: ChatMessage,
    onSpeakText: (String) -> Unit,
    onSaveAsNote: (String) -> Unit,
    onSaveAsReminder: (String) -> Unit
) {
    val isUser = message.role == "user"
    val align = if (isUser) Alignment.End else Alignment.Start
    
    // Apple classical bubble shapes
    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isUser) 16.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 16.dp
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_message_card"),
        horizontalAlignment = align
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp, top = 2.dp)
                        .size(24.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF00F0FF).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Cyclone,
                        contentDescription = null,
                        tint = Color(0xFF00F0FF),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Card(
                shape = shape,
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) Color(0xFF007AFF) else Color.White.copy(alpha = 0.08f) // iOS classic primary blue vs dark transparent grey
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isUser) "Me" else "Siri OS Voice",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isUser) Color.White.copy(alpha = 0.8f) else Color(0xFF00F0FF)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.text,
                        fontSize = 13.sp,
                        color = Color.White
                    )

                    if (!isUser) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.1f))
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = { onSpeakText(message.text) },
                                modifier = Modifier.height(28.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(11.dp), tint = Color(0xFF00F0FF))
                                    Text("Speak Voice", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                                }
                            }

                            TextButton(
                                onClick = { onSaveAsNote(message.text) },
                                modifier = Modifier.height(28.dp).testTag("save_as_note_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.StickyNote2, contentDescription = null, modifier = Modifier.size(11.dp), tint = Color(0xFFFFCC00))
                                    Text("Stick Note", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFCC00))
                                }
                            }

                            TextButton(
                                onClick = { onSaveAsReminder(message.text) },
                                modifier = Modifier.height(28.dp).testTag("save_as_task_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(11.dp), tint = Color(0xFF34C759))
                                    Text("Remind Me", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34C759))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// SCREEN B: Elegant Weather & News widget screen
// ==========================================
@Composable
fun WeatherNewsAppView(viewModel: AssistantViewModel) {
    val weatherData by viewModel.currentWeatherData.collectAsStateWithLifecycle()
    val newsArticles by viewModel.newsFeed.collectAsStateWithLifecycle()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // App Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "World Monitor",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "SYNCHRONIZED WITH WWW.WORLDMONITOR.APP",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFF9500),
                        letterSpacing = 0.5.sp
                    )
                }

                IconButton(
                    onClick = { viewModel.cycleWeather() },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Switch City Station",
                        tint = Color(0xFFFF9500),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Gorgeous Square/Wide iOS Weather Widget (2x2 representation)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = if (weatherData.condition == "Sunny") {
                                    listOf(Color(0xFF2979FF), Color(0xFF2196F3), Color(0xFF00B0FF))
                                } else if (weatherData.condition == "Drizzle") {
                                    listOf(Color(0xFF37474F), Color(0xFF546E7A), Color(0xFF78909C))
                                } else {
                                    listOf(Color(0xFF5E35B1), Color(0xFF7E57C2), Color(0xFFB39DDB))
                                }
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = weatherData.city,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = weatherData.condition,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            
                            // Huge climate symbol
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (weatherData.condition == "Sunny") {
                                        Icons.Default.WbSunny
                                    } else if (weatherData.condition == "Drizzle") {
                                        Icons.Default.CloudQueue
                                    } else {
                                        Icons.Default.Cloud
                                    },
                                    contentDescription = weatherData.condition,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = weatherData.temperature,
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Light,
                                color = Color.White
                            )
                            Text(
                                text = "Station calibrated\nvia WorldMonitor",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        Text(
                            text = weatherData.description.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.2f))
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Atmosphere specs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IosWeatherSpecElement(label = "HUMIDITY", value = weatherData.humidity)
                            IosWeatherSpecElement(label = "WIND SPEED", value = weatherData.wind)
                            IosWeatherSpecElement(label = "UV INDEX", value = weatherData.uvIndex)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Compact Forecast list
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            weatherData.forecast.forEach { f ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(f.day, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (f.condition == "Sunny") Icons.Default.WbSunny else Icons.Default.Cloud,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(f.temp, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // WWW.WORLDMONITOR.APP - World News headlines Widget
        item {
            Text(
                text = "Breaking News feed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (newsArticles.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
                ) {
                    Text(
                        "No breaking details. World Monitor station is idling nominal.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(newsArticles) { article ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    onClick = { 
                        viewModel.speak("Breaking story on " + article.title + ". " + article.summary)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFF9500).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = article.category,
                                    fontSize = 8.sp,
                                    color = Color(0xFFFF9500),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Text(
                                text = article.time,
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = article.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = article.summary,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = article.source,
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Read head aloud",
                                    tint = Color(0xFFFF9500),
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    "Tap card to hear details",
                                    fontSize = 9.sp,
                                    color = Color(0xFFFF9500),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IosWeatherSpecElement(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = Color.White,
            fontWeight = FontWeight.Black
        )
    }
}


// ==========================================
// SCREEN C: Apple iOS Reminders checklists
// ==========================================
@Composable
fun IosRemindersAppView(viewModel: AssistantViewModel) {
    val tasks by viewModel.tasksState.collectAsStateWithLifecycle()
    var isInsertingReminder by remember { mutableStateOf(false) }

    var draftTitle by remember { mutableStateOf("") }
    var draftDesc by remember { mutableStateOf("") }
    var draftPriority by remember { mutableStateOf("MEDIUM") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    draftTitle = ""
                    draftDesc = ""
                    draftPriority = "MEDIUM"
                    isInsertingReminder = true
                },
                containerColor = Color(0xFF34C759),
                contentColor = Color.White,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("add_task_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Reminder")
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Reminders",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "INTELLIGENT KATE OPERATIONAL CHECKLIST",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF34C759),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // iOS-style Category summary cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IosCounterCard(
                    title = "Today",
                    count = tasks.filter { !it.isCompleted }.size.toString(),
                    cardColor = Color(0xFF007AFF),
                    modifier = Modifier.weight(1.0f)
                )

                IosCounterCard(
                    title = "Finished",
                    count = tasks.filter { it.isCompleted }.size.toString(),
                    cardColor = Color(0xFF34C759),
                    modifier = Modifier.weight(1.0f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Reminder Items
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No pending reminders.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1.0f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks) { task ->
                        IosReminderCardItem(
                            task = task,
                            onToggle = { viewModel.toggleTaskCompletion(task) },
                            onDelete = { viewModel.delete_task(task) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        if (isInsertingReminder) {
            Dialog(onDismissRequest = { isInsertingReminder = false }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(18.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "New Reminder",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        OutlinedTextField(
                            value = draftTitle,
                            onValueChange = { draftTitle = it },
                            placeholder = { Text("Task Title *", color = Color.White.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF34C759),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = draftDesc,
                            onValueChange = { draftDesc = it },
                            placeholder = { Text("Reminder Description", color = Color.White.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF34C759),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("HIGH", "MEDIUM", "LOW").forEach { level ->
                                val selected = draftPriority == level
                                val tint = when (level) {
                                    "HIGH" -> Color.Red
                                    "MEDIUM" -> Color.Yellow
                                    else -> Color.Green
                                }
                                FilterChip(
                                    selected = selected,
                                    onClick = { draftPriority = level },
                                    label = { Text(level, fontSize = 9.sp, fontWeight = FontWeight.Black) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = tint.copy(alpha = 0.2f),
                                        selectedLabelColor = tint
                                    ),
                                    border = BorderStroke(1.dp, if (selected) tint else Color.Gray.copy(alpha = 0.3f))
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { isInsertingReminder = false }) {
                                Text("Dismiss", color = Color.White.copy(alpha = 0.6f))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (draftTitle.trim().isNotEmpty()) {
                                        viewModel.addTask(draftTitle, draftDesc, draftPriority)
                                        isInsertingReminder = false
                                        viewModel.speak("Reminder created successfully: ${draftTitle.trim()}")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                                modifier = Modifier.testTag("add_task_button")
                            ) {
                                Text("Create", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IosCounterCard(
    title: String,
    count: String,
    cardColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(50))
                        .background(cardColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = cardColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = count,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun IosReminderCardItem(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val tierColor = when (task.priority) {
        "HIGH" -> Color.Red
        "MEDIUM" -> Color.Yellow
        else -> Color.Green
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Validate Reminder Check",
                    tint = if (task.isCompleted) Color(0xFF34C759) else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1.0f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = if (task.isCompleted) Color.White.copy(alpha = 0.4f) else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(tierColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = task.priority,
                            fontSize = 7.sp,
                            color = tierColor,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                if (task.description.isNotEmpty()) {
                    Text(
                        text = task.description,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remove Reminder",
                    tint = Color.Red.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}


// ==========================================
// SCREEN D: Apple iOS Notes matrices
// ==========================================
@Composable
fun IosNotesAppView(viewModel: AssistantViewModel) {
    val notes by viewModel.notesState.collectAsStateWithLifecycle()
    var isInsertingNote by remember { mutableStateOf(false) }
    var selectedViewNote by remember { mutableStateOf<Note?>(null) }

    var draftTitle by remember { mutableStateOf("") }
    var draftContent by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    draftTitle = ""
                    draftContent = ""
                    isInsertingNote = true
                },
                containerColor = Color(0xFFFFCC00),
                contentColor = Color.Black,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("add_note_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note")
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Notes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "ENCRYPTED PERSONAL MEMORY MATRIX",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFFCC00),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notes representation
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Note,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No notes saved yet.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1.0f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notes) { note ->
                        IosNoteRowCard(
                            note = note,
                            onClick = { selectedViewNote = note },
                            onDelete = { viewModel.delete_note(note) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        if (isInsertingNote) {
            Dialog(onDismissRequest = { isInsertingNote = false }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(18.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Draft Note",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        OutlinedTextField(
                            value = draftTitle,
                            onValueChange = { draftTitle = it },
                            placeholder = { Text("Note Title *", color = Color.White.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFCC00),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = draftContent,
                            onValueChange = { draftContent = it },
                            placeholder = { Text("Write content details here...", color = Color.White.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFCC00),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 8
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { isInsertingNote = false }) {
                                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (draftTitle.trim().isNotEmpty() && draftContent.trim().isNotEmpty()) {
                                        viewModel.addNote(draftTitle, draftContent)
                                        isInsertingNote = false
                                        viewModel.speak("Note recorded: ${draftTitle.trim()}")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC00), contentColor = Color.Black),
                                modifier = Modifier.testTag("add_note_button")
                            ) {
                                Text("Engage", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        selectedViewNote?.let { note ->
            Dialog(onDismissRequest = { selectedViewNote = null }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(18.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Local memo decryption".uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFCC00)
                            )
                            IconButton(onClick = { selectedViewNote = null }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close View", tint = Color.White)
                            }
                        }

                        Text(
                            text = note.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )

                        Text(
                            text = note.content,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { 
                                    selectedViewNote = null
                                    viewModel.speak("Note details loaded. Title is ${note.title}")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC00), contentColor = Color.Black)
                            ) {
                                Text("Read Aloud", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IosNoteRowCard(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("note_item_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.StickyNote2,
                contentDescription = null,
                tint = Color(0xFFFFCC00),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1.0f)) {
                Text(
                    text = note.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = note.content,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remove Log Entry",
                    tint = Color.Red.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
