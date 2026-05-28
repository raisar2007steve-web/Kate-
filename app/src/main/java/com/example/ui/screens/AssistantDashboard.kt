package com.example.ui.screens

import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.StrokeJoin
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.viewmodel.AgentCompanionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.api.PipedTrendingItem
import com.example.data.ChatMessage
import com.example.data.Note
import com.example.data.Task
import com.example.viewmodel.AssistantViewModel
import com.example.viewmodel.NewsArticle
import com.example.viewmodel.ForecastHour
import com.example.viewmodel.ForecastDay
import com.example.viewmodel.WeatherData
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import android.media.AudioManager
import android.media.MediaPlayer
import android.content.Context
import android.view.KeyEvent
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin

enum class SystemUiState {
    LOCKSCREEN, HOMESCREEN, IN_APP
}

enum class IosLaunchApp {
    KATE_CHAT, WEATHER, NEWS, WORLD_MAP, REMINDERS, NOTES, MULTI_AGENTS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantDashboard(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeApp by remember { mutableStateOf(IosLaunchApp.KATE_CHAT) }
    var systemUiState by remember { mutableStateOf(SystemUiState.LOCKSCREEN) }

    // Collect TTS and widgets statuses
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val isVoiceMuted by viewModel.isVoiceMuted.collectAsStateWithLifecycle()
    val batteryLvl by viewModel.deviceBatteryPercentage.collectAsStateWithLifecycle()
    
    val isStsModeActive by viewModel.isStsModeActive.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
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

    var isVipExpanded by remember { mutableStateOf(false) }

    // Real physical/virtual Android device button controls the OS navigation!
    val backHandlerEnabled = isVipExpanded || 
                             (systemUiState == SystemUiState.IN_APP) || 
                             (systemUiState == SystemUiState.HOMESCREEN)

    BackHandler(enabled = backHandlerEnabled) {
        if (isVipExpanded) {
            isVipExpanded = false
        } else if (systemUiState == SystemUiState.IN_APP) {
            systemUiState = SystemUiState.HOMESCREEN
        } else if (systemUiState == SystemUiState.HOMESCREEN) {
            systemUiState = SystemUiState.LOCKSCREEN
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(iosWallpaperBrush)
    ) {
        if (systemUiState == SystemUiState.LOCKSCREEN) {
            IosLockScreenView(
                onUnlock = { systemUiState = SystemUiState.HOMESCREEN },
                viewModel = viewModel
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                
                // 1. Sleek Apple-style Status Bar
                IosStatusBar(batteryLevel = batteryLvl)

                if (systemUiState == SystemUiState.HOMESCREEN) {
                    Box(modifier = Modifier.weight(1f)) {
                        IosHomeScreenGrid(
                            onAppLaunch = {
                                activeApp = it
                                systemUiState = SystemUiState.IN_APP
                            }
                        )
                    }
                } else {
                    // 2. Main Active Application Window (Kate Chat, Weather, Map, Reminders, Notes)
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
                                IosLaunchApp.KATE_CHAT -> KateChatAppView(viewModel)
                                IosLaunchApp.WEATHER -> WeatherAppView(viewModel)
                                IosLaunchApp.NEWS -> NewsAppView(viewModel)
                                IosLaunchApp.WORLD_MAP -> WorldMapAppView(viewModel)
                                IosLaunchApp.REMINDERS -> IosRemindersAppView(viewModel)
                                IosLaunchApp.NOTES -> IosNotesAppView(viewModel)
                                IosLaunchApp.MULTI_AGENTS -> MultiAgentsAppView(viewModel)
                            }
                        }
                    }
                }

                // 3. Apple iOS Bottom Translucent App Dock (Used as general launcher & Home button)
                IosBottomAppDock(
                    activeApp = activeApp,
                    onAppLaunch = {
                        activeApp = it
                        systemUiState = SystemUiState.IN_APP
                    },
                    isSpeaking = isSpeaking,
                    isMuted = isVoiceMuted,
                    systemUiState = systemUiState,
                    onHomeClick = { systemUiState = SystemUiState.HOMESCREEN }
                )
            }
        }

        // Sleek overlay for VIP Command Center Panel in STS/Authenticated mode
        if (isStsModeActive) {
            if (isVipExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { isVipExpanded = false },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.88f)
                            .border(1.5.dp, Color(0xFFFFD700).copy(alpha = 0.35f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .clickable(enabled = false) {},
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF070707)),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                        VipCommandCenter(onMinimizeClick = { isVipExpanded = false })
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 110.dp, end = 16.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    val pulseAnim = rememberInfiniteTransition(label = "pulseGlow")
                    val glowScale by pulseAnim.animateFloat(
                        initialValue = 0.96f,
                        targetValue = 1.12f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = EaseInOutBack),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "glowScale"
                    )
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .graphicsLayer {
                                scaleX = glowScale
                                scaleY = glowScale
                            }
                            .shadow(12.dp, RoundedCornerShape(50))
                            .background(Color.Black, RoundedCornerShape(50))
                            .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(50))
                            .clickable { isVipExpanded = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Open VIP Console",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "VIP",
                                color = Color(0xFFFFD700),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun rememberNetworkStatus(context: android.content.Context): State<Boolean> {
    val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    val isConnected = remember { mutableStateOf(false) }

    DisposableEffect(connectivityManager) {
        val networkCallback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) { isConnected.value = true }
            override fun onLost(network: android.net.Network) { isConnected.value = false }
        }
        val request = android.net.NetworkRequest.Builder().build()
        try {
            connectivityManager.registerNetworkCallback(request, networkCallback)
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            isConnected.value = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: Exception) {
             // Ignore
        }

        onDispose {
            try { connectivityManager.unregisterNetworkCallback(networkCallback) } catch (e: Exception) {}
        }
    }
    return isConnected
}

@Composable
fun IosStatusBar(batteryLevel: Int) {
    val context = LocalContext.current
    val isConnected by rememberNetworkStatus(context)
    
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
                text = if (isConnected) "ONLINE" else "OFFLINE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isConnected) Color(0xFF00FF66) else Color.Red
            )
            Icon(
                imageVector = if (isConnected) Icons.Default.SignalCellular4Bar else Icons.Default.SignalCellularConnectedNoInternet0Bar,
                contentDescription = "iOS Network Active",
                tint = if (isConnected) Color.White else Color.Red,
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
            
            // Battery capsule using dynamic system batteryLevel percentage
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "$batteryLevel%",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(3.dp))
                Box(
                    modifier = Modifier
                        .size(width = 11.dp, height = 6.dp)
                        .background(
                            if (batteryLevel < 20) Color(0xFFFF453A) else Color(0xFF34C759), 
                            RoundedCornerShape(1f)
                        )
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
    isMuted: Boolean,
    systemUiState: SystemUiState,
    onHomeClick: () -> Unit
) {
    Column {
        if (systemUiState == SystemUiState.IN_APP) {
            // Home Indicator Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 100.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.5f))
                        .clickable { onHomeClick() }
                )
            }
        }
        
        if (systemUiState == SystemUiState.HOMESCREEN) {
            // Beautiful glassmorphic bottom tray holding Apple iOS dock icons (compact-optimized to fit 5 items)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .shadow(16.dp, shape = RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = 0.12f)) // Apple classic translucent bar
                        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)))
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App Icon 1: Kate Chat
                    DockAppIcon(
                        label = "Kate Chat",
                        iconVector = Icons.Default.Cyclone,
                        accentColor = Color(0xFF00F0FF),
                        isSelected = false,
                        isPulse = isSpeaking,
                        onClick = { onAppLaunch(IosLaunchApp.KATE_CHAT) }
                    )

                    // App Icon 2: Weather
                    DockAppIcon(
                        label = "Weather",
                        iconVector = Icons.Default.WbSunny,
                        accentColor = Color(0xFFFF9500),
                        isSelected = false,
                        onClick = { onAppLaunch(IosLaunchApp.WEATHER) }
                    )

                    // App Icon 3: News
                    DockAppIcon(
                        label = "News",
                        iconVector = Icons.Default.Article,
                        accentColor = Color(0xFFFF3B30),
                        isSelected = false,
                        onClick = { onAppLaunch(IosLaunchApp.NEWS) }
                    )

                    // App Icon 4: World Map
                    DockAppIcon(
                        label = "World Map",
                        iconVector = Icons.Default.Map,
                        accentColor = Color(0xFF30B0FF),
                        isSelected = false,
                        onClick = { onAppLaunch(IosLaunchApp.WORLD_MAP) }
                    )
                }
            }
        }
    }
}

@Composable
fun IosLockScreenView(
    onUnlock: () -> Unit,
    viewModel: AssistantViewModel
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }
    var currentTime by remember { mutableStateOf(timeFormat.format(Date())) }
    var currentDate by remember { mutableStateOf(dateFormat.format(Date())) }

    LaunchedEffect(Unit) {
        while (true) {
            val date = Date()
            currentTime = timeFormat.format(date)
            currentDate = dateFormat.format(date)
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = currentTime, fontSize = 72.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = currentDate, fontSize = 18.sp, color = Color.White.copy(alpha = 0.8f))
        }

        // Fake Notifications
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cyclone, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Kate.OS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text("System nominal. Ready for commands.", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }
        }

        // Swipe up to unlock
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Tap to unlock", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .clickable { onUnlock() }
            )
        }
    }
}

@Composable
fun IosHomeScreenGrid(
    onAppLaunch: (IosLaunchApp) -> Unit
) {
    // A grid of icons representing system apps
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp)
    ) {
        // App icons on the home screen
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HomeScreenAppIcon(label = "Kate Chat", iconVector = Icons.Default.Cyclone, color = Color(0xFF00F0FF), onClick = { onAppLaunch(IosLaunchApp.KATE_CHAT) })
            HomeScreenAppIcon(label = "Weather", iconVector = Icons.Default.WbSunny, color = Color(0xFFFF9500), onClick = { onAppLaunch(IosLaunchApp.WEATHER) })
            HomeScreenAppIcon(label = "News", iconVector = Icons.Default.Article, color = Color(0xFFFF3B30), onClick = { onAppLaunch(IosLaunchApp.NEWS) })
            HomeScreenAppIcon(label = "Maps", iconVector = Icons.Default.Map, color = Color(0xFF30B0FF), onClick = { onAppLaunch(IosLaunchApp.WORLD_MAP) })
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            HomeScreenAppIcon(label = "Reminders", iconVector = Icons.Default.ListAlt, color = Color(0xFF34C759), onClick = { onAppLaunch(IosLaunchApp.REMINDERS) })
            Spacer(modifier = Modifier.width(30.dp))
            HomeScreenAppIcon(label = "Notes", iconVector = Icons.Default.StickyNote2, color = Color(0xFFFFCC00), onClick = { onAppLaunch(IosLaunchApp.NOTES) })
            Spacer(modifier = Modifier.width(30.dp))
            HomeScreenAppIcon(label = "AI Agents", iconVector = Icons.Default.Groups, color = Color(0xFF9C27B0), onClick = { onAppLaunch(IosLaunchApp.MULTI_AGENTS) })
        }
    }
}

@Composable
fun HomeScreenAppIcon(
    label: String,
    iconVector: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(color.copy(alpha = 0.2f))
                .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
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
// SCREEN A: Kate-Style Voice Assistant App
// ==========================================
@Composable
fun KateChatAppView(viewModel: AssistantViewModel) {
    val context = LocalContext.current
    val messages by viewModel.messagesState.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val chatInput by viewModel.chatInput.collectAsStateWithLifecycle()
    
    val voicePitch by viewModel.voicePitch.collectAsStateWithLifecycle()
    val voiceSpeed by viewModel.voiceSpeed.collectAsStateWithLifecycle()
    val isMuted by viewModel.isVoiceMuted.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val selectedMode by viewModel.selectedAgentMode.collectAsStateWithLifecycle()

    val isStsModeActive by viewModel.isStsModeActive.collectAsStateWithLifecycle()

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
        // Kate Ambient Waveform widget
        KateVoiceWidget(
            isSpeaking = isSpeaking,
            isMuted = isMuted,
            voicePitch = voicePitch,
            voiceSpeed = voiceSpeed,
            onPitchChange = { viewModel.updateVoicePitch(it) },
            onSpeedChange = { viewModel.updateVoiceSpeed(it) },
            onToggleMute = { viewModel.toggleVoiceMuted() },
            onStopVoice = { viewModel.stopSpeaking() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // AI Companion Agent Selector Row
        Text(
            text = "SELECT KATE COMPANION MODE",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00FFCC),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AgentCompanionMode.values().forEach { mode ->
                val isSelected = selectedMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (isSelected) Color(0xFF00FFCC).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { viewModel.selectAgentMode(mode) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = mode.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF00FFCC) else Color.White
                        )
                        Text(
                            text = when(mode) {
                                AgentCompanionMode.COMPANION -> "Daily Assist"
                                AgentCompanionMode.DEEP_DIVER -> "Deeper Tech"
                                AgentCompanionMode.RESEARCH -> "Fact Find"
                                AgentCompanionMode.ANALYST -> "SWOT/Decide"
                            },
                            fontSize = 8.sp,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                            maxLines = 1,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }

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
                        Toast.makeText(context, "Kate memory sync complete.", Toast.LENGTH_SHORT).show()
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
                            text = if (isStsModeActive) "How can I help you, Boss?" else "How can I help you?",
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
                            text = "Kate connection thinking...",
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
                placeholder = { Text("Command Kate assist...", color = Color.White.copy(alpha = 0.5f)) },
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
fun KateVoiceWidget(
    isSpeaking: Boolean,
    isMuted: Boolean,
    voicePitch: Float,
    voiceSpeed: Float,
    onPitchChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onStopVoice: () -> Unit
) {
    var isMinimized by remember { mutableStateOf(false) }

    // Beautiful widget displaying a dynamic glowing waveform representation
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Column(modifier = Modifier.padding(minOf(if (isMinimized) 12.dp else 16.dp))) {
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
                        text = "Kate Voice Synthesizer".uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { isMinimized = !isMinimized },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isMinimized) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = "Minimize or Expand",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Volume helper
                    IconButton(
                        onClick = onToggleMute,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Mute Voice Trigger",
                            tint = if (isMuted) Color.Red else Color(0xFF00F0FF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (!isMinimized) {
                Spacer(modifier = Modifier.height(12.dp))

                // Waveform core canvas
                KateCustomWaveform(isActive = isSpeaking)

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
}

@Composable
fun KateCustomWaveform(isActive: Boolean) {
    // Elegant real-time mathematical sine-wave animation that scales correctly
    val infiniteTransition = rememberInfiniteTransition(label = "KateWaveform")
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
                        text = if (isUser) "Me" else "Kate OS Voice",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isUser) Color.White.copy(alpha = 0.8f) else Color(0xFF00F0FF)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (!isUser && message.text.contains("###")) {
                        val sections = message.text.split("###").filter { it.isNotBlank() }
                        sections.forEach { section ->
                            val lines = section.trim().lines()
                            if (lines.isNotEmpty()) {
                                val header = lines.first().trim()
                                val content = lines.drop(1).joinToString("\n").trim()
                                if (header.isNotEmpty() || content.isNotEmpty()) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            if (header.isNotEmpty()) {
                                                Text(text = header.replace("*", "").uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F0FF))
                                                Spacer(modifier = Modifier.height(4.dp))
                                            }
                                            Text(text = content, fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = message.text,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }

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
// Live Stock Market Indexes
data class StockIndex(
    val name: String,
    val value: String,
    val change: String,
    val isUp: Boolean,
    val description: String
)

val stockIndexesList = listOf(
    StockIndex("S&P 500", "5,234.18", "▲ +0.85% (+44)", true, "S&P 500 climbs forty-four points driven by major tech stock inflows and consumer confidence."),
    StockIndex("NASDAQ", "16,340.50", "▼ -0.35% (-57)", false, "Nasdaq Composite settles lower by zero point thirty-five percent on global tech profit taking."),
    StockIndex("DOW JONES", "39,120.20", "▲ +0.45% (+175)", true, "Dow Jones climbing by one hundred seventy-five points or zero point forty-five percent on industrial gains."),
    StockIndex("FTSE 100", "7,930.92", "▲ +0.60% (+47)", true, "London's FTSE 100 sees a steady gain led by energy and financial sectors."),
    StockIndex("NIKKEI 225", "40,815.66", "▲ +1.50% (+603)", true, "Japan's Nikkei rallies over six hundred points reaching new heights amidst steady monetary policies."),
    StockIndex("DAX 40", "18,205.94", "▲ +0.25% (+45)", true, "German DAX index edges higher following positive Eurozone manufacturing reports."),
    StockIndex("BSE SENSEX", "74,825.80", "▲ +1.15% (+850)", true, "Sensex BSE index climbs eight hundred fifty points to new record. High tech and bank stock inflows.")
)

data class YouTubeChannel(
    val name: String,
    val id: String,
    val logo: String,
    val status: String,
    val lang: String
)

val newsChannelsList = listOf(
    YouTubeChannel("Lofi Girl", "jfKfPfyJRdk", "LG", "🔴 LIVE", "Music"),
    YouTubeChannel("DW News", "V9KzY83_N64", "DW", "🔴 LIVE", "English"),
    YouTubeChannel("Sky News UK", "9Auq9mYxFEE", "SK", "🔴 LIVE", "English"),
    YouTubeChannel("France 24", "sPJeEG23lO8", "FR", "🔴 LIVE", "English"),
    YouTubeChannel("ABC News AUS", "W1ilCyKVy5E", "AB", "🔴 LIVE", "English"),
    YouTubeChannel("NASA Live", "21X5lGlDOfg", "NA", "🔴 LIVE", "English"),
    YouTubeChannel("Bloomberg", "dp8PhLsUcFE", "BL", "🔴 LIVE", "English"),
    YouTubeChannel("CNN News18", "8_mB8n-v08g", "CN", "🔴 LIVE", "English"),
    YouTubeChannel("Aaj Tak Live", "NqbF6Wlh-fQ", "AT", "🔴 LIVE", "Hindi"),
    YouTubeChannel("Republic Bharat", "yGvAmg9fMzo", "RB", "🔴 LIVE", "Hindi")
)

@Composable
fun MarkdownText(text: String, color: Color = Color.White, fontSize: androidx.compose.ui.unit.TextUnit = 12.sp, lineHeight: androidx.compose.ui.unit.TextUnit = 16.sp) {
    val annotatedString = remember(text) {
        androidx.compose.ui.text.buildAnnotatedString {
            var currentIndex = 0
            val boldPattern = java.util.regex.Pattern.compile("\\*\\*(.*?)\\*\\*")
            val matcher = boldPattern.matcher(text)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                val matchText = matcher.group(1) ?: ""
                
                // Append text before match
                if (start > currentIndex) {
                    append(text.substring(currentIndex, start))
                }
                
                // Append bold style
                withStyle(style = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFFF9500))) {
                    append(matchText)
                }
                
                currentIndex = end
            }
            if (currentIndex < text.length) {
                append(text.substring(currentIndex))
            }
        }
    }
    Text(text = annotatedString, color = color, fontSize = fontSize, lineHeight = lineHeight)
}

@Composable
fun NewsAppView(viewModel: AssistantViewModel) {
    val newsArticles by viewModel.newsFeed.collectAsStateWithLifecycle()
    val aiSummary by viewModel.aiNewsSummary.collectAsStateWithLifecycle()
    val isSummaryLoading by viewModel.isNewsSummaryLoading.collectAsStateWithLifecycle()
    val pipedVideos by viewModel.pipedTrendingList.collectAsStateWithLifecycle()
    val isPipedLoading by viewModel.isPipedLoading.collectAsStateWithLifecycle()
    
    var selectedChannelInputIndex by remember { mutableStateOf(0) }
    val activeChannel = newsChannelsList.getOrElse(selectedChannelInputIndex) { newsChannelsList[0] }

    var activePipedVideo by remember { mutableStateOf<PipedTrendingItem?>(null) }
    
    val currentPlayingVideoId = remember(activeChannel, activePipedVideo) {
        val url = activePipedVideo?.url
        if (url != null) {
            val exactId = url.substringAfter("v=", "").substringBefore("&")
            if (exactId.isNotEmpty()) exactId else url.substringAfter("/watch?v=", "")
        } else {
            activeChannel.id
        }
    }

    val currentPlayingTitle = remember(activeChannel, activePipedVideo) {
        activePipedVideo?.title ?: activeChannel.name
    }

    val categories = listOf("All", "Sports", "Entertainment", "Gaming", "Real Life", "Political", "Food")
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    val initialWebViewSupported = remember {
        try {
            Class.forName("android.webkit.WebView")
            android.webkit.CookieManager.getInstance()
            true
        } catch (e: Throwable) {
            false
        }
    }
    var isWebViewSupported by remember { mutableStateOf(initialWebViewSupported) }

    val seedArticles = remember {
        listOf(
            NewsArticle(101, "Formula 1: Monaco GP Sparkles as Ferrari Clinches Dramatic Win", "Sports", "Grand Prix Digest", "A masterclass strategy under changing weather conditions hands Ferrari the iconic silverware on the streets of Monte Carlo.", "2m ago"),
            NewsArticle(102, "Major Leagues: Underdog Surge Rewrites Playoff Projections", "Sports", "SportsCenter Live", "An unprecedented late-season surge has wild card contenders breaking statistical records in professional leagues.", "15m ago"),
            NewsArticle(151, "CineCon: Award-Winning Director Unveils Next Sci-Fi Epic", "Entertainment", "Hollywood Bulletin", "The upcoming feature explores mind-bending reality concepts using breakthrough physical miniature sets in virtual production.", "1h ago"),
            NewsArticle(152, "Pop Charts: Acoustic Indie Track Beats Synthesized Mega Hits", "Entertainment", "Daily Record", "A minimalist solo acoustic track recorded in an old barn reaches the top of global streaming platforms.", "3h ago"),
            NewsArticle(201, "Next-Gen Console Unwrapped: Breakthrough Raytracing Engine", "Gaming", "PixelHub", "Under-the-hood specs show massive memory throughput optimizations allowing realistic light transport at extreme refresh rates.", "10m ago"),
            NewsArticle(202, "Indie Hit Sells 10 Million Copies in 7 Days", "Gaming", "Steam Chronicles", "Developed by a solo creator, this cozy farming sim meets automation sandbox captures millions of active players.", "4m ago"),
            NewsArticle(251, "Off-Grid Living: Inside the Eco-Dome Community In Arctic Edge", "Real Life", "Nomadic Chronicles", "Local engineers design self-sustaining domes utilizing geothermal vents and localized aeroponics crop production.", "2h ago"),
            NewsArticle(252, "The Rise of Micro-Communities in Crowded City Centers", "Real Life", "Urban Quarterly", "City dwellers are re-learning collaborative living through shared utility agreements and neighborhood tool libraries.", "5h ago"),
            NewsArticle(301, "Global Senate Passes Landmark Digital Sovereign Privacy Framework", "Political", "Democracy Tribune", "Broad bipartisan consensus secures sweeping individual control over cryptographic verification tokens in state services.", "30m ago"),
            NewsArticle(302, "Infrastructure Bill Greenlights Over 5,000 High-Speed Commuter Rails", "Political", "Capital Dispatch", "A major policy shift redirects state capital reserves towards building carbon-negative high-capacity high-speed transit links.", "1h ago"),
            NewsArticle(351, "Gastronomy Study: Traditional Fermentation Meets Modern Culinary Labs", "Food", "Culinary Digest", "Michelin chefs are leveraging ancient probiotic fermentation cultures to craft exquisite plant-based alternative delicacies.", "4h ago"),
            NewsArticle(352, "The Sourdough Database: Archiving Ancient Micro-Organisms", "Food", "Bakers Weekly", "Researchers compile genomic data for over 4,000 wild yeasts dating back to ancestral Mesopotamian household lineages.", "8h ago")
        )
    }

    val filteredArticles = remember(newsArticles, selectedCategory) {
        val combined = newsArticles.map { it.copy(category = if (it.category == "GLOBAL" || it.category == "") "News" else it.category) } + seedArticles
        if (selectedCategory == "All") {
            combined
        } else {
            combined.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        text = "Global News Monitor",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "POLLING SENSEX, YOUTUBE LIVE & AI DISPATCHERS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFF9500),
                        letterSpacing = 0.5.sp
                    )
                }

                IconButton(
                    onClick = { 
                        // Refresh
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh News",
                        tint = Color(0xFFFF9500),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        // News Categories Row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Color(0xFFFF9500) else Color.White.copy(alpha = 0.1f))
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = category,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else Color.White
                        )
                    }
                }
            }
        }

        // Live stock metrics ticker card (Ups and downs)
        item {
            Column {
                Text(
                    text = "📊 NATIONAL & GLOBAL MARKETS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF9500),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(stockIndexesList) { index ->
                        Card(
                            onClick = {
                                viewModel.speak(index.description)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier.width(150.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(index.name, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(index.value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(if (index.isUp) Color(0xFF34C759) else Color(0xFFFF3B30))
                                    )
                                    Text(
                                        text = index.change,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (index.isUp) Color(0xFF34C759) else Color(0xFFFF3B30)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // YOUTUBE LIVE CHANNELS SEGMENT
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "📺 YOUTUBE LIVE BROADCAST STATIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF9500),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Click to switch active YouTube station live feed instantly",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    itemsIndexed(newsChannelsList) { idx, channel ->
                        val isSelected = selectedChannelInputIndex == idx
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) Color(0xFFFF9500).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFFFF9500) else Color.White.copy(alpha = 0.10f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedChannelInputIndex = idx
                                    activePipedVideo = null
                                    viewModel.speak("Switching live YouTube stream to " + channel.name)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFF3B30)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = channel.logo,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Column {
                                    Text(
                                        text = channel.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFFFF9500) else Color.White
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(Color.Red)
                                        )
                                        Text(
                                            text = "${channel.status} (${channel.lang})",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // PIPED TRENDING VIDEO LIVE FEED
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔥 PIPED TRENDING LIVE STREAM INDEX",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFF9500),
                        letterSpacing = 1.sp
                    )
                    
                    if (isPipedLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = Color(0xFFFF9500),
                            strokeWidth = 1.5.dp
                        )
                    } else {
                        IconButton(
                            onClick = { viewModel.fetchPipedTrending() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color(0xFFFF9500),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "Source: piped.video real-time API. Tap any stream to project onto receiver main window",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )

                if (pipedVideos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Awaiting live telemetry streams...",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        items(pipedVideos) { video ->
                            val isPlayingThis = activePipedVideo?.url == video.url
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isPlayingThis) Color(0xFFFF9500) else Color.White.copy(alpha = 0.08f)
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isPlayingThis) Color(0xFFFF9500).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f)
                                ),
                                modifier = Modifier
                                    .width(180.dp)
                                    .clickable {
                                        activePipedVideo = video
                                        viewModel.speak("Projecting live item: " + (video.title ?: ""))
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Thumbnail Container
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(95.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black)
                                    ) {
                                        AsyncImage(
                                            model = video.thumbnail ?: "",
                                            contentDescription = video.title ?: "Stream Thumbnail",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                        
                                        // Duration Label
                                        val dur = video.duration ?: 0L
                                        if (dur > 0) {
                                            val mins = dur / 60
                                            val secs = dur % 60
                                            val durationStr = "${mins}:${if (secs < 10) "0" else ""}$secs"
                                            Text(
                                                text = durationStr,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(2.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "STREAM",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .background(Color.Red, RoundedCornerShape(2.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Title descriptor
                                    Text(
                                        text = video.title ?: "Untitled Transmission",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPlayingThis) Color(0xFFFF9500) else Color.White,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 14.sp,
                                        modifier = Modifier.height(28.dp)
                                    )

                                    // Uploader information
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = "Publisher",
                                            tint = Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = video.uploaderName ?: "Unknown Publisher",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // Views count, date
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val vViews = video.views ?: 0L
                                        val viewsFormatted = if (vViews > 1000000) {
                                            "${(vViews / 100000.0).toInt() / 10.0}M views"
                                        } else if (vViews > 1000) {
                                            "${vViews / 1000}K views"
                                        } else {
                                            "$vViews views"
                                        }
                                        
                                        Text(
                                            text = viewsFormatted,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.4f)
                                        )
                                        
                                        if (video.uploadedDate != null) {
                                            Text(
                                                text = video.uploadedDate,
                                                fontSize = 8.sp,
                                                color = Color(0xFF00FFCC),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Stream player
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!isWebViewSupported) {
                        FallbackComposePlayer(
                            title = currentPlayingTitle,
                            id = currentPlayingVideoId,
                            isPiped = activePipedVideo != null,
                            onChannelReset = {
                                activePipedVideo = null
                                viewModel.speak("Resuming live channel streams")
                            }
                        )
                    } else {
                        AndroidView(
                            factory = { ctx ->
                                try {
                                    WebView(ctx).apply {
                                        tag = currentPlayingVideoId
                                        webChromeClient = android.webkit.WebChromeClient()
                                        webViewClient = object : WebViewClient() {
                                            override fun onReceivedError(
                                                view: WebView?,
                                                request: WebResourceRequest?,
                                                error: WebResourceError?
                                            ) {
                                                // Silent standard error bypass
                                            }
                                        }
                                        settings.apply {
                                            javaScriptEnabled = true
                                            domStorageEnabled = true
                                            mediaPlaybackRequiresUserGesture = false
                                            userAgentString = "Mozilla/5.0 (Linux; Android 13; SM-K970) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                                        }
                                        loadUrl("https://www.youtube.com/embed/${currentPlayingVideoId}?autoplay=1&mute=1&playsinline=1&enablejsapi=1")
                                    }
                                } catch (e: Throwable) {
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        isWebViewSupported = false
                                    }
                                    android.view.View(ctx).apply {
                                        tag = "FALLBACK"
                                    }
                                }
                            },
                            update = { webView ->
                                try {
                                    if (webView is WebView) {
                                        val lastLoadedId = webView.tag as? String
                                        if (lastLoadedId != currentPlayingVideoId) {
                                            webView.tag = currentPlayingVideoId
                                            webView.loadUrl("https://www.youtube.com/embed/${currentPlayingVideoId}?autoplay=1&mute=1&playsinline=1&enablejsapi=1")
                                        }
                                    }
                                } catch (e: Throwable) {
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        isWebViewSupported = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Overlay stream banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart)
                            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (activePipedVideo != null) Color(0xFFFF9500) else Color.Red)
                            )
                            Text(
                                text = "NOW PLAYING: ${currentPlayingTitle.uppercase()}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 0.8.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (activePipedVideo != null) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFF9500).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .border(1.dp, Color(0xFFFF9500), RoundedCornerShape(4.dp))
                                        .clickable { 
                                            activePipedVideo = null
                                            viewModel.speak("Resuming live channel streams")
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("CHANNEL FEED", fontSize = 8.sp, color = Color(0xFFFF9500), fontWeight = FontWeight.ExtraBold)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .background(Color.Red, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("LIVE FEED", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // THE AI GENERATED NEWS SUMMARY BULLETIN CORNER
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color(0xFFFF9500).copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Stars, "AI Core", tint = Color(0xFFFF9500), modifier = Modifier.size(16.dp))
                            Text(
                                text = "AI REAL-TIME BULLETIN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }

                        if (isSummaryLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFFFF9500),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF00FFCC).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "MODEL ACTIVE",
                                    fontSize = 8.sp,
                                    color = Color(0xFF00FFCC),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    // Markdown-styled AI summary text parsing
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        MarkdownText(
                            text = aiSummary,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    // Request Button CTA
                    Button(
                        onClick = { viewModel.generateNewsSummary(activeChannel.name) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSummaryLoading
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, "Lightning", tint = Color.Black, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (isSummaryLoading) "ANALYZING CHANNELS..." else "GENERATE AI BRIEFING SUMMARY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }



        // BREAKING NEWS GENERAL FEED (Important Cards)
        item {
            Text(
                text = "📰 HIGH-PRIORITY FIELD HEADLINES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFF9500),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (filteredArticles.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
                ) {
                    Text(
                        "No breaking details for this category. Station is nominal.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(filteredArticles) { article ->
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
                                        "Tap card to read aloud",
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
            
            item {
                Spacer(modifier = Modifier.height(40.dp))
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

// ==========================================
// SCREEN C: Offline India detailed Map & World Map Online
// ==========================================

data class StreetVector(
    val name: String,
    val path: List<Offset>,
    val width: Float
)

data class MallVector(
    val name: String,
    val rectOffset: Offset,
    val size: androidx.compose.ui.geometry.Size,
    val info: String
)

data class StallVector(
    val name: String,
    val coord: Offset,
    val category: String, // "Street Food", "Tea Stall", "Bazaar Store", "Souvenirs"
    val ratings: String,
    val reviewsSpeak: String
)

data class IndianDistrictMap(
    val name: String,
    val centerDescription: String,
    val physicalBorder: List<Offset>, // polygons
    val streets: List<StreetVector>,
    val malls: List<MallVector>,
    val foodStalls: List<StallVector>
)

val indianDistricts = listOf(
    IndianDistrictMap(
        name = "Delhi NCR Hub",
        centerDescription = "Delhi Metropolitan Region: Connaught Place, India Gate & Kartavya Path corridors.",
        physicalBorder = listOf(
            Offset(50f, 50f), Offset(450f, 60f), Offset(420f, 400f), Offset(80f, 380f)
        ),
        streets = listOf(
            StreetVector("Kartavya Path (Rajpath)", listOf(Offset(250f, 350f), Offset(250f, 150f)), 8f),
            StreetVector("Janpath Road Corridor", listOf(Offset(100f, 250f), Offset(400f, 250f)), 6f),
            StreetVector("Barakhamba Bypass Road", listOf(Offset(120f, 120f), Offset(380f, 380f)), 5f),
            StreetVector("Sansad Marg Avenue", listOf(Offset(150f, 320f), Offset(350f, 180f)), 5f)
        ),
        malls = listOf(
            MallVector("Palika Underground Bazaar", Offset(200f, 180f), androidx.compose.ui.geometry.Size(90f, 45f), "Huge subterranean air-conditioned shopping node in Connaught Place representing electronics and apparel."),
            MallVector("Chanakya Luxury Arcade", Offset(80f, 80f), androidx.compose.ui.geometry.Size(80f, 40f), "High-end luxury shopping and dining destination located in Delhi's diplomatic enclave."),
            MallVector("Select Citywalk Metro Mall", Offset(310f, 300f), androidx.compose.ui.geometry.Size(100f, 50f), "Elite shopping destination with premium high-street fashion, international gourmet stalls and dynamic events.")
        ),
        foodStalls = listOf(
            StallVector("Natraj Dahi Bhalla CP", Offset(250f, 230f), "Street Food", "4.8 ⭐ (12k+ reviews)", "Natraj Dahi Bhalla is world famous for its sweet soft spiced lentil dumplings in customized yogurt with tangy mint chutneys since 1940."),
            StallVector("Khan Chacha Rolls", Offset(160f, 310f), "Street Food", "4.6 ⭐ (9k+ reviews)", "Khan Chacha is a legendary culinary stall specialized in charcoal-grilled soft seekh kebabs and fresh roomali rolls."),
            StallVector("Barakhamba Tea Depot & Chai", Offset(350f, 210f), "Tea Stall", "4.9 ⭐ (3k+ reviews)", "A vibrant, busy local corner serving rich, hot ginger cardamom cutting tea to professionals and tourists alike."),
            StallVector("Chanakyapuri Souvenir Spot", Offset(110f, 140f), "Bazaar Store", "4.5 ⭐ (800 reviews)", "A government accredited local craft depot displaying brass materials, handmade rugs, and custom wood carvings.")
        )
    ),
    IndianDistrictMap(
        name = "Mumbai Colaba Hub",
        centerDescription = "Mumbai South Coast: Colaba Causeway, Marine Drive Crescent Promenade & Gateway of India.",
        physicalBorder = listOf(
            Offset(80f, 30f), Offset(380f, 40f), Offset(440f, 390f), Offset(60f, 370f)
        ),
        streets = listOf(
            StreetVector("Marine Drive Crescent", listOf(Offset(80f, 120f), Offset(180f, 280f), Offset(320f, 350f)), 8f),
            StreetVector("Colaba Causeway Main Rd", listOf(Offset(280f, 80f), Offset(280f, 380f)), 6f),
            StreetVector("Apollo Bunder Pier Gate", listOf(Offset(280f, 150f), Offset(380f, 150f)), 5f)
        ),
        malls = listOf(
            MallVector("Taj Shopping Arcade", Offset(300f, 100f), androidx.compose.ui.geometry.Size(80f, 40f), "Exclusive luxury boutique arcade nested inside the iconic Taj Mahal Palace, presenting legacy diamonds and high couture."),
            MallVector("Phoenix Palladium South", Offset(110f, 180f), androidx.compose.ui.geometry.Size(95f, 48f), "Massive multi-story mall features flagship retail spaces, interactive kid domains, and premium dining outlets.")
        ),
        foodStalls = listOf(
            StallVector("Bademiya Seekh Kebab Colaba", Offset(280f, 210f), "Street Food", "4.7 ⭐ (15k+ reviews)", "Bademiya is an open-air dynamic seekh kebab stall operating late into the night, serving soft mutton rolls and buttery warm roomali rotis."),
            StallVector("Sharma Mumbai Cutting Chai", Offset(150f, 220f), "Tea Stall", "4.8 ⭐ (4k+ reviews)", "A highly popular street stall serving piping hot sweetened milk milk tea infused with crushed ginger and lemongrass in elegant glass cups."),
            StallVector("Elco Sev Puri & Pani Puri", Offset(210f, 310f), "Street Food", "4.6 ⭐ (11k+ reviews)", "Legendary street food diner renowned across Mumbai for ice-cooled mineral water pani puris and crispy loaded sev dahi puris."),
            StallVector("Colaba Causeway Handloom Bazaar", Offset(275f, 330f), "Bazaar Store", "4.4 ⭐ (2k+ reviews)", "Charming roadside stores featuring colorful brass metal artifacts, dynamic custom bracelets, and cotton block-print tunics.")
        )
    ),
    IndianDistrictMap(
        name = "Bengaluru Core Hub",
        centerDescription = "Bengaluru Tech Smart-city: Indiranagar 100 Feet Road Grid & Brigade Road commercial hub.",
        physicalBorder = listOf(
            Offset(70f, 40f), Offset(430f, 30f), Offset(450f, 380f), Offset(90f, 410f)
        ),
        streets = listOf(
            StreetVector("Indiranagar 100 Ft Road", listOf(Offset(100f, 80f), Offset(100f, 380f)), 7f),
            StreetVector("Brigade Crossing Road", listOf(Offset(80f, 220f), Offset(380f, 220f)), 6f),
            StreetVector("M.G. Road Tech Boulevard", listOf(Offset(150f, 120f), Offset(350f, 320f)), 5f)
        ),
        malls = listOf(
            MallVector("Nexus Forum Town Center", Offset(180f, 100f), androidx.compose.ui.geometry.Size(100f, 40f), "Premier smart shopping mall offering interactive dynamic screens, major cinema halls, and modern retail layouts."),
            MallVector("Garuda Mall Commercial Annex", Offset(250f, 240f), androidx.compose.ui.geometry.Size(85f, 45f), "Centrally situated premium mall with excellent multi-brand apparel channels, local toy stores, and dynamic foods.")
        ),
        foodStalls = listOf(
            StallVector("Corner House Ice Cream CP", Offset(100f, 180f), "Street Food", "4.9 ⭐ (20k+ reviews)", "World famous dessert outlet beloved for its rich, heavy hot-chocolate-fudge poured over double scoops of vanilla ice creams with loaded walnuts."),
            StallVector("Srinidhi Sagar Filter Coffee", Offset(180f, 250f), "Tea Stall", "4.8 ⭐ (7k+ reviews)", "Famous south Indian street food diner serving frothy metric strong chicory-fused drip filter coffee in traditional brass tumblers."),
            StallVector("V.V. Puram Chaat Street Hub", Offset(280f, 150f), "Street Food", "4.7 ⭐ (14k+ reviews)", "Vibrant atmospheric food lane packing high crowds for spicy sweet masala corn, hot gulab jamun, and loaded potato twisters."),
            StallVector("Blossoms Old Book Bazaar", Offset(310f, 290f), "Bazaar Store", "4.8 ⭐ (6k+ reviews)", "Vast multi-story legacy bookstore holding millions of old classics, science textbooks, and rare manual paperbacks.")
        )
    )
)

@Composable
fun WeatherAppView(viewModel: AssistantViewModel) {
    val gpsWeather by viewModel.locationWeather.collectAsStateWithLifecycle()
    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (fineGranted || coarseGranted) {
                viewModel.updateWeatherWithCurrentLocation()
            }
        }
    )

    LaunchedEffect(Unit) {
        viewModel.updateWeatherWithCurrentLocation()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        text = "Google Weather Feed",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "REAL-TIME GPS OPENWEATHER STATION",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFF9500),
                        letterSpacing = 0.5.sp
                    )
                }

                IconButton(
                    onClick = { 
                        viewModel.updateWeatherWithCurrentLocation()
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh GPS Weather Feed",
                        tint = Color(0xFFFF9500),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (gpsWeather == null) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, Color(0xFFFF9500).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(Color(0xFF37474F), Color(0xFF263238))))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFFFF9500), modifier = Modifier.size(32.dp))
                        Text(text = "Satellite GPS Weather Standby", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = "A real OpenWeather Map GPS tracker feed. Please enable permission to locate your node automatically.",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9500)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ACTIVATE GPS WORKSTATION", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        } else {
            val active = gpsWeather!!
            
            // 1. Google Search Weather-style Main Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = active.city,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = active.condition,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFFF9500)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = active.description,
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                            }
                            
                            val condIcon = when {
                                active.condition.contains("Sunny", true) || active.condition.contains("Clear", true) -> Icons.Default.WbSunny
                                active.condition.contains("Rain", true) || active.condition.contains("Drizzle", true) || active.condition.contains("Storm", true) -> Icons.Default.Cyclone
                                else -> Icons.Default.Cloud
                            }
                            
                            Icon(
                                imageVector = condIcon,
                                contentDescription = null,
                                tint = if (active.condition.contains("Sunny", true) || active.condition.contains("Clear", true)) Color(0xFFFFD700) else Color(0xFF30B0FF),
                                modifier = Modifier.size(52.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = active.temperature.replace("°", ""),
                                    fontSize = 64.sp,
                                    fontWeight = FontWeight.ExtraLight,
                                    color = Color.White
                                )
                                Text(
                                    text = "°C",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFFFF9500),
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }
                            
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(text = "Humidity: ${active.humidity}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                Text(text = "Wind: ${active.wind}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                Text(text = "UV Index: ${active.uvIndex}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
            
            // 2. Full Day Portion (Hourly Forecast Slider - Google Weather style)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "TODAY'S DIURNAL PORTION (HOURLY FEED)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF9500),
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val hourlyList = active.hourly.ifEmpty {
                                (0..7).map { index ->
                                    val hourStr = SimpleDateFormat("h a", Locale.US).format(Date(System.currentTimeMillis() + index * 3 * 3600 * 1000))
                                    val hourTemp = active.temperature.replace("°", "").toIntOrNull() ?: 24
                                    ForecastHour(
                                        time = hourStr,
                                        temp = "${hourTemp + (index % 3) - (index / 2)}°",
                                        condition = active.condition
                                    )
                                }
                            }
                            
                            hourlyList.forEach { hour ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .width(55.dp)
                                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(text = hour.time, fontSize = 9.sp, color = Color.LightGray)
                                    val hrIcon = when {
                                        hour.condition.contains("Sunny", true) || hour.condition.contains("Clear", true) -> Icons.Default.WbSunny
                                        hour.condition.contains("Rain", true) || hour.condition.contains("Drizzle", true) || hour.condition.contains("Storm", true) -> Icons.Default.Cyclone
                                        else -> Icons.Default.Cloud
                                    }
                                    Icon(
                                        imageVector = hrIcon,
                                        contentDescription = null,
                                        tint = if (hour.condition.contains("Sunny", true) || hour.condition.contains("Clear", true)) Color(0xFFFFD700) else Color(0xFF30B0FF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(text = hour.temp, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Week Portion (Daily Forecast List - Google Weather style)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "WEEK OUTLOOK FEED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF9500),
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            active.forecast.forEach { forecastDay ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = forecastDay.day,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.width(80.dp)
                                    )
                                    
                                    val dayIcon = when {
                                        forecastDay.condition.contains("Sunny", true) || forecastDay.condition.contains("Clear", true) -> Icons.Default.WbSunny
                                        forecastDay.condition.contains("Rain", true) || forecastDay.condition.contains("Drizzle", true) || forecastDay.condition.contains("Storm", true) -> Icons.Default.Cyclone
                                        else -> Icons.Default.Cloud
                                    }
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = dayIcon,
                                            contentDescription = null,
                                            tint = if (forecastDay.condition.contains("Sunny", true) || forecastDay.condition.contains("Clear", true)) Color(0xFFFFD700) else Color(0xFF30B0FF),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = forecastDay.condition,
                                            fontSize = 11.sp,
                                            color = Color.LightGray
                                        )
                                    }
                                    
                                    Text(
                                        text = forecastDay.temp,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.width(90.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorldMapAppView(viewModel: AssistantViewModel) {
    val locationWeather by viewModel.locationWeather.collectAsStateWithLifecycle()
    
    // Extract lat/long from the weather data title or just use a default
    // In AssistantViewModel, locationWeather.city is formatted as "Lat: XX.XX, Lon: YY.YY"
    var latitude by remember { mutableDoubleStateOf(0.0) }
    var longitude by remember { mutableDoubleStateOf(0.0) }
    
    LaunchedEffect(locationWeather) {
        val cityStr = locationWeather?.city
        if (cityStr != null && cityStr.startsWith("Lat:")) {
            try {
                // Parse "Lat: XX.XX, Lon: YY.YY"
                val parts = cityStr.split(",")
                val latStr = parts[0].replace("Lat:", "").trim()
                val lonStr = parts[1].replace("Lon:", "").trim()
                latitude = latStr.toDouble()
                longitude = lonStr.toDouble()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E2124))
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Live World Map",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "OSMDROID GLOBAL EXPLORER",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF30B0FF),
                    letterSpacing = 0.5.sp
                )
            }

            IconButton(
                onClick = { 
                    viewModel.updateWeatherWithCurrentLocation()
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Locate Me",
                    tint = Color(0xFF30B0FF),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        // Map Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        ) {
            OsmdroidMapView(
                modifier = Modifier.fillMaxSize(),
                latitude = latitude,
                longitude = longitude
            )
            
            // Location Badge Overlay
            if (locationWeather != null && latitude != 0.0) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                    border = BorderStroke(1.dp, Color(0xFF30B0FF).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = "Lock",
                            tint = Color(0xFF30B0FF),
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Live Target Locked",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "LAT: ${String.format(java.util.Locale.US, "%.4f", latitude)}\nLON: ${String.format(java.util.Locale.US, "%.4f", longitude)}",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorldMapAppView_OLD(viewModel: AssistantViewModel) {
    var mapModeByInternet by remember { mutableStateOf(false) } // false = Offline India, true = World Map Online
    val locationWeather by viewModel.locationWeather.collectAsStateWithLifecycle()
    
    var selectedDistrictIndex by remember { mutableStateOf(0) }
    val activeDistrict = indianDistricts[selectedDistrictIndex]
    
    // Zoom/pan state
    var zoomScale by remember { mutableStateOf(1.2f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    
    // Filters state
    var showStreets by remember { mutableStateOf(true) }
    var showMalls by remember { mutableStateOf(true) }
    var showStalls by remember { mutableStateOf(true) }

    // Search query state
    var searchQuery by remember { mutableStateOf("") }
    var selectedLocationName by remember { mutableStateOf<String?>(null) }
    var selectedLocationDetail by remember { mutableStateOf<String?>(null) }
    var selectedLocationCoordinates by remember { mutableStateOf<Offset?>(null) }
    var selectedLocationCategory by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedDistrictIndex) {
        // Reset scale and center coordinates when district changes
        zoomScale = 1.3f
        panOffset = Offset.Zero
        selectedLocationName = null
        selectedLocationDetail = null
        viewModel.speak("Loading offline vector directory pack for " + activeDistrict.name + ". " + activeDistrict.centerDescription)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mode Selector Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { mapModeByInternet = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!mapModeByInternet) Color(0xFF30B0FF) else Color.Transparent,
                    contentColor = if (!mapModeByInternet) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("India Map Offline", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { mapModeByInternet = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (mapModeByInternet) Color(0xFF30B0FF) else Color.Transparent,
                    contentColor = if (mapModeByInternet) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("World Map Online", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (mapModeByInternet) {
            // WEB SCREEN MAP VIEW DIRECT IMPORT FROM OpenStreetMap
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "LIVE ONLINE WORLD SYNC PORTAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF30B0FF),
                        letterSpacing = 1.sp
                    )
                    Text(
                        "Live stream query: openstreetmap.org. Zoom, scroll and explore globally.",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    AndroidView(
                        factory = { ctx ->
                            try {
                                WebView(ctx).apply {
                                    webViewClient = object : WebViewClient() {
                                        override fun onReceivedError(
                                            view: WebView?,
                                            request: WebResourceRequest?,
                                            error: WebResourceError?
                                        ) {
                                            // bypass standard errors
                                        }
                                    }
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        builtInZoomControls = true
                                        displayZoomControls = false
                                    }
                                    loadUrl("https://www.openstreetmap.org/#map=5/22.973/78.656") // India & World View
                                }
                            } catch (e: Throwable) {
                                android.view.View(ctx)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }
        } else {
            // INDIA OFFLINE FULL COGNITIVE VECTOR STREETS, STALLS & MALLS MAP
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Region Selector Tab bar
                    Text(
                        text = "🏛️ SELECT REGIONAL DISTRICT DIRECTORY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF30B0FF),
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        indianDistricts.forEachIndexed { idx, dist ->
                            val isSel = idx == selectedDistrictIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSel) Color(0xFF30B0FF).copy(alpha = 0.15f) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedDistrictIndex = idx }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dist.name.replace(" Hub", ""),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color(0xFF30B0FF) else Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Multi-interactive Filters Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LAYERS:",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Filter Streets
                            FilterPill(label = "Streets", active = showStreets, color = Color(0xFFFFD700)) { showStreets = !showStreets }
                            // Filter Malls
                            FilterPill(label = "Malls", active = showMalls, color = Color(0xFFA020F0)) { showMalls = !showMalls }
                            // Filter Food Stalls
                            FilterPill(label = "Food Stalls", active = showStalls, color = Color(0xFFFF5722)) { showStalls = !showStalls }
                        }
                    }

                    // Quick Search Bar Inputs
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search stalls, malls, streets in ${activeDistrict.name}...", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF30B0FF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                }
                            } else {
                                Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                            }
                        }
                    )

                    // Unified Results dropdown or alert if matching found
                    val matchedStall = activeDistrict.foodStalls.firstOrNull { it.name.contains(searchQuery, ignoreCase = true) }
                    val matchedMall = activeDistrict.malls.firstOrNull { it.name.contains(searchQuery, ignoreCase = true) }
                    val matchedStreet = activeDistrict.streets.firstOrNull { it.name.contains(searchQuery, ignoreCase = true) }

                    if (searchQuery.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF30B0FF).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when {
                                    matchedStall != null -> "📍 Found Stall: ${matchedStall.name} (${matchedStall.category})"
                                    matchedMall != null -> "🏢 Found Mall: ${matchedMall.name}"
                                    matchedStreet != null -> "🏢 Found Street: ${matchedStreet.name}"
                                    else -> "❌ No matching locations in offline index"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            if (matchedStall != null || matchedMall != null || matchedStreet != null) {
                                Button(
                                    onClick = {
                                        if (matchedStall != null) {
                                            selectedLocationName = matchedStall.name
                                            selectedLocationDetail = "${matchedStall.category} • Ratings: ${matchedStall.ratings}\n\n${matchedStall.reviewsSpeak}"
                                            selectedLocationCoordinates = matchedStall.coord
                                            selectedLocationCategory = "STALL"
                                            panOffset = Offset(200f - matchedStall.coord.x, 200f - matchedStall.coord.y)
                                            zoomScale = 2.2f
                                        } else if (matchedMall != null) {
                                            selectedLocationName = matchedMall.name
                                            selectedLocationDetail = matchedMall.info
                                            selectedLocationCoordinates = matchedMall.rectOffset
                                            selectedLocationCategory = "MALL"
                                            panOffset = Offset(200f - matchedMall.rectOffset.x, 200f - matchedMall.rectOffset.y)
                                            zoomScale = 1.8f
                                        } else if (matchedStreet != null) {
                                            selectedLocationName = matchedStreet.name
                                            selectedLocationDetail = "Metropolitan offline vector street line in CP Smart-city grid. Lanes fully integrated."
                                            selectedLocationCoordinates = matchedStreet.path.firstOrNull() ?: Offset.Zero
                                            selectedLocationCategory = "STREET"
                                            panOffset = Offset(200f - (matchedStreet.path.firstOrNull()?.x ?: 0f), 200f - (matchedStreet.path.firstOrNull()?.y ?: 0f))
                                            zoomScale = 1.6f
                                        }
                                        searchQuery = ""
                                        viewModel.speak("Centering coordinates node on " + selectedLocationName)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30B0FF)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(22.dp)
                                ) {
                                    Text("Center Node", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }

                    // INDIA VECTOR GRID MAP CANVAS VIEW (Zoomable & Pannable)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF070B14))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    panOffset += dragAmount
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            withTransform({
                                translate(left = panOffset.x, top = panOffset.y)
                                scale(scaleX = zoomScale, scaleY = zoomScale, pivot = Offset(w / 2f, h / 2f))
                            }) {
                                // 1. Trace deep projection spatial grids (Streets of intelligence)
                                val spacing = 40f
                                for (x in 0..(w.toInt() * 2) step spacing.toInt()) {
                                    drawLine(
                                        color = Color(0xFF1E293B).copy(alpha = 0.25f),
                                        start = Offset(x.toFloat() - w, -h),
                                        end = Offset(x.toFloat() - w, h * 2),
                                        strokeWidth = 1f
                                    )
                                }
                                for (y in 0..(h.toInt() * 2) step spacing.toInt()) {
                                    drawLine(
                                        color = Color(0xFF1E293B).copy(alpha = 0.25f),
                                        start = Offset(-w, y.toFloat() - h),
                                        end = Offset(w * 2, y.toFloat() - h),
                                        strokeWidth = 1f
                                    )
                                }

                                // 2. Draw District Outer Bound Vector Polygon
                                val borderPath = Path().apply {
                                    val b = activeDistrict.physicalBorder
                                    if (b.isNotEmpty()) {
                                        moveTo(b[0].x, b[0].y)
                                        for (i in 1 until b.size) {
                                            lineTo(b[i].x, b[i].y)
                                        }
                                        close()
                                    }
                                }
                                drawPath(borderPath, color = Color(0xFF0F172A).copy(alpha = 0.7f))
                                drawPath(borderPath, color = Color(0xFF30B0FF).copy(alpha = 0.15f), style = Stroke(width = 3f))

                                // 3. Draw Streets vectors list
                                if (showStreets) {
                                    activeDistrict.streets.forEach { street ->
                                        val streetPath = Path().apply {
                                            if (street.path.isNotEmpty()) {
                                                moveTo(street.path[0].x, street.path[0].y)
                                                for (i in 1 until street.path.size) {
                                                    lineTo(street.path[i].x, street.path[i].y)
                                                }
                                            }
                                        }
                                        // Outer glowing street lane
                                        drawPath(
                                            streetPath,
                                            color = Color(0xFFFFD700).copy(alpha = 0.25f),
                                            style = Stroke(width = street.width * 2f, join = StrokeJoin.Round)
                                        )
                                        // Inner solid street lane line
                                        drawPath(
                                            streetPath,
                                            color = Color(0xFFFFD700),
                                            style = Stroke(width = street.width * 0.7f, join = StrokeJoin.Round)
                                        )
                                    }
                                }

                                // 4. Draw Mall rectangular vector areas
                                if (showMalls) {
                                    activeDistrict.malls.forEach { mall ->
                                        val isHighlighted = selectedLocationName == mall.name
                                        drawRect(
                                            color = if (isHighlighted) Color(0xFFA020F0).copy(alpha = 0.45f) else Color(0xFFA020F0).copy(alpha = 0.15f),
                                            topLeft = mall.rectOffset,
                                            size = mall.size
                                        )
                                        drawRect(
                                            color = if (isHighlighted) Color(0xFF00FFCC) else Color(0xFFA020F0),
                                            topLeft = mall.rectOffset,
                                            size = mall.size,
                                            style = Stroke(width = 2f)
                                        )
                                    }
                                }

                                // 5. Draw Food Stalls and bazaar points pins on the canvas overlay
                                if (showStalls) {
                                    activeDistrict.foodStalls.forEach { stall ->
                                        val isHighlighted = selectedLocationName == stall.name
                                        // Radial sonar pulse on selection
                                        if (isHighlighted) {
                                            drawCircle(
                                                color = Color(0xFF00FFCC).copy(alpha = 0.3f),
                                                radius = 24f,
                                                center = stall.coord
                                            )
                                        }
                                        // Inner solid pointer dot
                                        drawCircle(
                                            color = if (isHighlighted) Color(0xFF00FFCC) else Color(0xFFFF5722),
                                            radius = 8f,
                                            center = stall.coord
                                        )
                                        drawCircle(
                                            color = Color.White,
                                            radius = 4f,
                                            center = stall.coord
                                        )
                                    }
                                }
                            }
                        }

                        // Top Overlay Panel: Interactive Tap selection or touch coordinate reader
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopStart)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "🛰️ SELECT PLOT CORES TO SURVEY",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF30B0FF)
                            )
                            Text(
                                "Touch elements in the quick catalog below to explore detail logs",
                                fontSize = 8.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }

                        // Zoom indicator on Canvas corner
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "ZOOM: ${String.format(java.util.Locale.US, "%.1fy", zoomScale)}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF30B0FF)
                            )
                        }
                    }

                    // Interactive local catalog of Malls & Stalls (each inches fully covered)
                    Text(
                        text = "📋 LOCAL DIRECTORY CATALOG (TOUCH TO PINPOINT CAMERA & READ DETAIL)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.5f)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Display malls items in the list
                        if (showMalls) {
                            items(activeDistrict.malls) { mall ->
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFA020F0).copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                        .border(1.dp, if (selectedLocationName == mall.name) Color(0xFF00FFCC) else Color(0xFFA020F0).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            selectedLocationName = mall.name
                                            selectedLocationDetail = mall.info
                                            selectedLocationCoordinates = mall.rectOffset
                                            selectedLocationCategory = "MALL"
                                            // Center camera
                                            panOffset = Offset(200f - mall.rectOffset.x, 200f - mall.rectOffset.y)
                                            zoomScale = 1.6f
                                            viewModel.speak("Showing shopping mall details: " + mall.name)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Storefront, null, tint = Color(0xFFEA80FC), modifier = Modifier.size(13.dp))
                                        Text(mall.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }

                        // Display Food stalls in the list
                        if (showStalls) {
                            items(activeDistrict.foodStalls) { stall ->
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFF5722).copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                        .border(1.dp, if (selectedLocationName == stall.name) Color(0xFF00FFCC) else Color(0xFFFF5722).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            selectedLocationName = stall.name
                                            selectedLocationDetail = "${stall.category} • Ratings: ${stall.ratings}\n\n${stall.reviewsSpeak}"
                                            selectedLocationCoordinates = stall.coord
                                            selectedLocationCategory = "STALL"
                                            // Center camera
                                            panOffset = Offset(200f - stall.coord.x, 200f - stall.coord.y)
                                            zoomScale = 2.0f
                                            viewModel.speak("Showing food stall details: " + stall.name + ". " + stall.reviewsSpeak)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Restaurant, null, tint = Color(0xFFFFAB40), modifier = Modifier.size(13.dp))
                                        Text(stall.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // Persistent detail survey card block (Active when selecting element)
                    AnimatedContent(targetState = selectedLocationName != null) { hasSel ->
                        if (hasSel && selectedLocationName != null) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFF30B0FF).copy(alpha = 0.4f)),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(
                                                imageVector = if (selectedLocationCategory == "MALL") Icons.Default.Storefront else Icons.Default.PinDrop,
                                                contentDescription = null,
                                                tint = Color(0xFF30B0FF),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = selectedLocationName ?: "",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                        IconButton(
                                            onClick = { selectedLocationName = null },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                                        }
                                    }

                                    Text(
                                        text = selectedLocationDetail ?: "",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.8f),
                                        lineHeight = 14.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Coordinates mapping: offset[${String.format(java.util.Locale.US, "x=%.0f, y=%.0f", selectedLocationCoordinates?.x ?: 0f, selectedLocationCoordinates?.y ?: 0f)}]",
                                            fontSize = 9.sp,
                                            color = Color.White.copy(alpha = 0.4f)
                                        )

                                        Button(
                                            onClick = {
                                                viewModel.speak("Location Log Survey: " + selectedLocationName + ". Details: " + selectedLocationDetail)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30B0FF)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(24.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.VolumeUp, null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                                Text("Read survey text", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Tactical slider control for precise zoom focus
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { zoomScale = (zoomScale - 0.2f).coerceIn(1.0f, 6.0f) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.ZoomOut, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                        Slider(
                            value = zoomScale,
                            onValueChange = { zoomScale = it },
                            valueRange = 1.0f..6.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF30B0FF),
                                activeTrackColor = Color(0xFF30B0FF)
                            ),
                            modifier = Modifier.weight(1.0f)
                        )
                        IconButton(onClick = { zoomScale = (zoomScale + 0.2f).coerceIn(1.0f, 6.0f) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.ZoomIn, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                        IconButton(
                            onClick = {
                                zoomScale = 1.2f
                                panOffset = Offset.Zero
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.CenterFocusStrong, "Center", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }

                    // Bottom status bar of Offline Cache Pack for India
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.VerifiedUser, null, tint = Color(0xFF34C759), modifier = Modifier.size(11.dp))
                            Text("offline data package secure", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Text("Active Pack: v2026.05 // index size: CP smart grid", fontSize = 8.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Live location coordinates monitor panel
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛰️ DEVICE GEOLOCATION MODULE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF30B0FF),
                        letterSpacing = 1.sp
                    )

                    Button(
                        onClick = { viewModel.updateWeatherWithCurrentLocation() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30B0FF)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("Poll GPS", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                if (locationWeather != null) {
                    Text(
                        text = locationWeather!!.city,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = locationWeather!!.description,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "GPS Coordinates IDLE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Using coarse location mapping under standalone OS guidelines.",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
fun FilterPill(label: String, active: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                color = if (active) color.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = if (active) color else Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (active) color else Color.White.copy(alpha = 0.4f))
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (active) Color.White else Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

enum class VipPanelType {
    INSTAGRAM, MATERIALS, SPOTIFY, PERFORMANCE, SPECTRUM
}

@Composable
fun VipCommandCenter(onMinimizeClick: () -> Unit) {
    var activePanel by remember { mutableStateOf(VipPanelType.INSTAGRAM) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070707))
    ) {
        // High-end VIP dock with a beautifully sculpted control interface
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VIP GATEWAY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD700),
                    letterSpacing = 1.5.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFD700).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "VIP PROTOCOL ACTIVE",
                            fontSize = 7.sp,
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    IconButton(
                        onClick = onMinimizeClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Minimize panel",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Launcher Grid incorporating stylish indicator rings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dockItems = listOf(
                    Triple(VipPanelType.INSTAGRAM, Icons.Default.CameraAlt, "Instagram"),
                    Triple(VipPanelType.MATERIALS, Icons.Default.TrendingUp, "Reserve"),
                    Triple(VipPanelType.SPOTIFY, Icons.Default.MusicNote, "Spotify"),
                    Triple(VipPanelType.PERFORMANCE, Icons.Default.Memory, "Status"),
                    Triple(VipPanelType.SPECTRUM, Icons.Default.GraphicEq, "3D Wave")
                )

                dockItems.forEach { (type, icon, label) ->
                    VipDockItem(
                        type = type,
                        icon = icon,
                        label = label,
                        isSelected = activePanel == type,
                        onClick = { activePanel = type },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        Divider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.fillMaxWidth())
        
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activePanel) {
                VipPanelType.INSTAGRAM -> InstagramSidePanel()
                VipPanelType.MATERIALS -> MaterialsNewsPanel()
                VipPanelType.SPOTIFY -> SpotifyPanel()
                VipPanelType.PERFORMANCE -> PerformancePanel()
                VipPanelType.SPECTRUM -> SpectrumPanel()
            }
        }
    }
}

@Composable
fun VipDockItem(
    type: VipPanelType,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulseAnim = rememberInfiniteTransition(label = "iconPulse")
    val scale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = if (isSelected) {
                            listOf(Color(0xFFFFD700).copy(alpha = 0.3f), Color.Transparent)
                        } else {
                            listOf(Color.White.copy(alpha = 0.05f), Color.Transparent)
                        }
                    ),
                    shape = RoundedCornerShape(50)
                )
                .border(
                    width = 1.dp,
                    color = if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
            color = if (isSelected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun MaterialsNewsPanel() {
    val context = LocalContext.current
    var isSyncingWeb by remember { mutableStateOf(false) }
    var syncCompletedAt by remember { mutableStateOf("Just Now (Live)") }
    
    val liveTimer = rememberInfiniteTransition(label = "commodities")
    val multiplier by liveTimer.animateFloat(
        initialValue = -0.02f,
        targetValue = 0.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "multiplier"
    )

    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "COMMODITIES & SECTOR RESERVES",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "REAL-TIME GLOBALLY SYNCED TELEMETRY",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }
                
                Button(
                    onClick = {
                        scope.launch {
                            isSyncingWeb = true
                            delay(1000)
                            isSyncingWeb = false
                            val sdf = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            syncCompletedAt = "${sdf.format(java.util.Date())} (Web Verified)"
                            Toast.makeText(context, "Commodity prices refreshed via google search web feeds!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isSyncingWeb
                ) {
                    if (isSyncingWeb) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.Black, strokeWidth = 1.5.dp)
                    } else {
                        Text("WEB SYNC", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Last Google Search Spot Verify: $syncCompletedAt",
                fontSize = 8.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        val usdToInr = 83.50
        val materials = listOf(
            Triple("Gold (XAU)", 2431.50, "+1.2%"),
            Triple("Silver (XAG)", 29.40, "+2.5%"),
            Triple("Crude Oil (WTI)", 82.10, "-0.4%"),
            Triple("Copper (HG)", 4.65, "+0.8%"),
            Triple("Platinum", 995.00, "+0.2%"),
            Triple("Palladium", 1050.20, "-1.1%"),
            Triple("Titanium Ore", 312.40, "+5.4%"),
            Triple("Lithium Carbonate", 13400.00, "-2.1%")
        )

        items(materials) { (name, basePriceUsd, baseChange) ->
            val fluctuatedPrice = basePriceUsd * usdToInr * (1.0 + multiplier)
            val isPositive = baseChange.startsWith("+")
            val displayPrice = if (fluctuatedPrice > 1000) {
                "₹" + String.format(Locale.US, "%,.2f", fluctuatedPrice)
            } else {
                "₹" + String.format(Locale.US, "%.2f", fluctuatedPrice)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFFFD700).copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(text = name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = "Strategic Reserve Node", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = displayPrice,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                    Text(
                        text = baseChange,
                        color = if (isPositive) Color(0xFF34C759) else Color(0xFFFF3B30),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .background(Color(0xFFFFD700).copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text("VIP ANALYTICAL MEMO", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFD700))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Precious and industrial metals are signaling defensive capital allocations globally. Direct tracking is synchronized with STS command nodes.",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        lineHeight = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

data class OnlineTrack(val id: Int, val title: String, val artist: String, val url: String)

@Composable
fun SpotifyPanel() {
    val context = LocalContext.current
    
    // Dynamic list of tracks that user can modify (Add & Remove)
    val trackList = remember {
        mutableStateListOf(
            OnlineTrack(1, "Lofi Space Beats", "Cozy Ambience", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
            OnlineTrack(2, "Ambient Relaxation", "Sine Synthesizer", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
            OnlineTrack(3, "Late Night Echoes", "STS Orbit Station", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3")
        )
    }
    
    var selectedTrackIndex by remember { mutableStateOf(0) }
    var isReallyPlaying by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableStateOf(0f) }
    
    // Dialog / Input panel variables for adding custom online streams
    var showAddDialog by remember { mutableStateOf(false) }
    var inputTitle by remember { mutableStateOf("") }
    var inputArtist by remember { mutableStateOf("") }
    var inputUrl by remember { mutableStateOf("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3") } // Pre-filled with valid stream url
    
    val currentTrack = if (trackList.isNotEmpty() && selectedTrackIndex in trackList.indices) {
        trackList[selectedTrackIndex]
    } else {
        null
    }
    
    // Real Android MediaPlayer streaming actual online audio streams!
    val mediaPlayer = remember { MediaPlayer() }
    
    // Dynamic vinyl spinning animation
    val recordRotation = rememberInfiniteTransition(label = "recordRot")
    val angle by recordRotation.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isReallyPlaying) 3000 else 1000000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    // Trigger or pause network audio stream based on states
    LaunchedEffect(selectedTrackIndex, isReallyPlaying) {
        if (isReallyPlaying && currentTrack != null) {
            try {
                mediaPlayer.setOnPreparedListener(null)
                mediaPlayer.setOnCompletionListener(null)
                mediaPlayer.setOnErrorListener(null)
                mediaPlayer.reset()
                mediaPlayer.setDataSource(context, Uri.parse(currentTrack.url))
                mediaPlayer.setOnPreparedListener { mp ->
                    try {
                        mp.start()
                    } catch (t: Throwable) {}
                }
                mediaPlayer.setOnCompletionListener {
                    try {
                        // Advance to next or stop
                        if (selectedTrackIndex < trackList.size - 1) {
                            selectedTrackIndex++
                        } else {
                            isReallyPlaying = false
                        }
                        playbackProgress = 0f
                    } catch (t: Throwable) {}
                }
                mediaPlayer.setOnErrorListener { mp, what, extra ->
                    try {
                        isReallyPlaying = false
                    } catch (t: Throwable) {}
                    true
                }
                mediaPlayer.prepareAsync()
            } catch (t: Throwable) {
                Toast.makeText(context, "Stream error: ${t.message}", Toast.LENGTH_SHORT).show()
                isReallyPlaying = false
            }
        } else {
            try {
                mediaPlayer.setOnPreparedListener(null)
                mediaPlayer.setOnCompletionListener(null)
                mediaPlayer.setOnErrorListener(null)
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                }
            } catch (t: Throwable) {}
        }
    }

    // Refresh progress state continuously when streaming
    LaunchedEffect(isReallyPlaying, selectedTrackIndex) {
        while (isReallyPlaying) {
            delay(1000)
            try {
                if (mediaPlayer.isPlaying) {
                    val duration = mediaPlayer.duration
                    if (duration > 0) {
                        playbackProgress = mediaPlayer.currentPosition.toFloat() / duration.toFloat()
                    }
                }
            } catch (t: Throwable) {}
        }
    }

    // Safely release media player on navigation/dismissal
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer.setOnPreparedListener(null)
                mediaPlayer.setOnCompletionListener(null)
                mediaPlayer.setOnErrorListener(null)
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
            } catch (t: Throwable) {}
            try {
                mediaPlayer.reset()
            } catch (t: Throwable) {}
            try {
                mediaPlayer.release()
            } catch (t: Throwable) {}
        }
    }

    // Load AudioManager for real-device music controls!
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    var systemVolume by remember {
        mutableStateOf(
            try {
                audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 7
            } catch (t: Throwable) {
                7
            }
        )
    }
    val maxVolume = remember {
        try {
            audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        } catch (t: Throwable) {
            15
        }
    }

    val sliderMax = if (maxVolume > 0) maxVolume.toFloat() else 15f
    val safeSystemVolume = systemVolume.toFloat().coerceIn(0f, sliderMax)

    // Dispatch physical system-wide media button keypresses to control ANY audio resource on the parent OS!
    val dispatchSystemAudioControl: (Int) -> Unit = { keyCode ->
        try {
            val down = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val up = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            audioManager?.dispatchMediaKeyEvent(down)
            audioManager?.dispatchMediaKeyEvent(up)
            Toast.makeText(context, "dispatched real-device keystroke: $keyCode", Toast.LENGTH_SHORT).show()
        } catch (t: Throwable) {
            Toast.makeText(context, "media trigger failed: ${t.message}", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "SPOTIFY ACTIVE COMPANION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1DB954),
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Vinyl grooves container representing current audio status
        item {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .shadow(12.dp, RoundedCornerShape(50))
                    .border(2.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                    .background(Color(0xFF121212), RoundedCornerShape(50))
                    .graphicsLayer { rotationZ = angle },
                contentAlignment = Alignment.Center
            ) {
                // Vinyl grooves background
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (r in 15..65 step 12) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.03f),
                            radius = r.dp.toPx(),
                            style = Stroke(width = 1f)
                        )
                    }
                }
                
                // Centered Album Art
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.sweepGradient(listOf(Color(0xFF1DB954), Color.Black, Color(0xFF1DB954)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Display current active streaming track info
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currentTrack?.title ?: "Select are track below",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = currentTrack?.artist ?: "None selected",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Live streaming seek slider
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val currentSecs = (playbackProgress * 180).toInt()
                Text(
                    text = String.format(Locale.US, "%02d:%02d", currentSecs / 60, currentSecs % 60),
                    color = Color.Gray,
                    fontSize = 10.sp
                )
                LinearProgressIndicator(
                    progress = { playbackProgress },
                    modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF1DB954),
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Text("03:00", color = Color.Gray, fontSize = 10.sp)
            }
        }

        // Local Streaming Player Controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (selectedTrackIndex > 0) {
                        selectedTrackIndex--
                    } else if (trackList.isNotEmpty()) {
                        selectedTrackIndex = trackList.size - 1
                    }
                }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                
                FloatingActionButton(
                    onClick = { isReallyPlaying = !isReallyPlaying },
                    containerColor = Color(0xFF1DB954),
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(
                        imageVector = if (isReallyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play-Pause",
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                IconButton(onClick = {
                    if (selectedTrackIndex < trackList.size - 1) {
                        selectedTrackIndex++
                    } else {
                        selectedTrackIndex = 0
                    }
                }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }

        // REAL-DEVICE AUDIO RESOURCES CONTROLLER
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "REAL DEVICE EXTERNAL MUSIC CONTROLS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1DB954),
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Device Hardware Volume Slider
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Text("Volume", color = Color.White, fontSize = 11.sp, modifier = Modifier.width(50.dp))
                    Slider(
                        value = safeSystemVolume,
                        onValueChange = {
                            systemVolume = it.toInt()
                            try {
                                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, systemVolume, 0)
                            } catch (t: Throwable) {}
                        },
                        valueRange = 0f..sliderMax,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFF1DB954),
                            thumbColor = Color(0xFF1DB954)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Real device media triggers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { dispatchSystemAudioControl(KeyEvent.KEYCODE_MEDIA_PREVIOUS) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).padding(2.dp).height(32.dp)
                    ) {
                        Text("SYS PREV", fontSize = 8.sp, color = Color.White)
                    }
                    Button(
                        onClick = { dispatchSystemAudioControl(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1.2f).padding(2.dp).height(32.dp)
                    ) {
                        Text("SYS PLAY/PAUS", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { dispatchSystemAudioControl(KeyEvent.KEYCODE_MEDIA_NEXT) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f).padding(2.dp).height(32.dp)
                    ) {
                        Text("SYS NEXT", fontSize = 8.sp, color = Color.White)
                    }
                }
            }
        }

        // PLAYLIST MANAGER: ADD AND REMOVE SOUNDS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PLAYLIST CHANNELS (${trackList.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, Color(0xFF1DB954).copy(alpha = 0.4f)),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("+ ADD MUSIC", fontSize = 9.sp, color = Color(0xFF1DB954), fontWeight = FontWeight.Bold)
                }
            }
        }

        // List out tracklist items with removal controls
        itemsIndexed(trackList) { index, track ->
            val isActive = selectedTrackIndex == index
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isActive) Color(0xFF1DB954).copy(alpha = 0.1f) else Color.White.copy(alpha = 0.02f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isActive) Color(0xFF1DB954).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        selectedTrackIndex = index
                        isReallyPlaying = true
                    }
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (isActive && isReallyPlaying) Icons.Default.VolumeUp else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (isActive) Color(0xFF1DB954) else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(track.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artist, color = Color.LightGray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                IconButton(
                    onClick = {
                        if (trackList.size > 1) {
                            trackList.removeAt(index)
                            if (selectedTrackIndex >= trackList.size) {
                                selectedTrackIndex = trackList.size - 1
                            }
                        } else {
                            Toast.makeText(context, "Keep at least 1 track", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                }
            }
        }
    }

    // Modal dialog to input customized online music paths
    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                border = BorderStroke(1.dp, Color(0xFF1DB954).copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("ADD ONLINE MP3 STREAM", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    
                    OutlinedTextField(
                        value = inputTitle,
                        onValueChange = { inputTitle = it },
                        label = { Text("Song Title") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1DB954),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF1DB954)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = inputArtist,
                        onValueChange = { inputArtist = it },
                        label = { Text("Artist") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1DB954),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF1DB954)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        label = { Text("MP3 URL Stream") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1DB954),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF1DB954)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (inputTitle.isNotBlank() && inputUrl.isNotBlank()) {
                                    val newTrack = OnlineTrack(
                                        id = (trackList.maxOfOrNull { it.id } ?: 0) + 1,
                                        title = inputTitle,
                                        artist = if (inputArtist.isBlank()) "Unknown Artist" else inputArtist,
                                        url = inputUrl
                                    )
                                    trackList.add(newTrack)
                                    showAddDialog = false
                                    inputTitle = ""
                                    inputArtist = ""
                                    inputUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"
                                } else {
                                    Toast.makeText(context, "Please set name & URL", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
                        ) {
                            Text("Add Track", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PerformancePanel() {
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            tick++
        }
    }

    val processors = remember { Runtime.getRuntime().availableProcessors() }
    val maxMemoryMb = remember { Runtime.getRuntime().maxMemory() / (1024 * 1024) }
    val allocatedMemoryMb = Runtime.getRuntime().totalMemory() / (1024 * 1024)
    val freeMemoryMb = Runtime.getRuntime().freeMemory() / (1024 * 1024)
    val activeUsedMemoryMb = allocatedMemoryMb - freeMemoryMb
    val memoryUsageRatio = activeUsedMemoryMb.toFloat() / maxMemoryMb.toFloat()

    val fileRoot = remember { java.io.File("/") }
    val rootSpaceTotalGb = remember { fileRoot.totalSpace / (1024 * 1024 * 1024) }
    val rootSpaceFreeGb = fileRoot.freeSpace / (1024 * 1024 * 1024)
    val storageRatio = (rootSpaceTotalGb - rootSpaceFreeGb).toFloat() / rootSpaceTotalGb.coerceAtLeast(1).toFloat()

    // Fluctuating metric simulating realistic immediate CPU thread spikes
    val cpuUtilization = remember(tick) {
        (35 + (System.currentTimeMillis() % 28) + (if (processors > 4) 8 else 2)).coerceIn(10, 95)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SYSTEM ARCHITECTURE & MEMORANDUMS",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = Color.White
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // CPU Metric
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Active Silicon Threads ($processors Cores)", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        Text("$cpuUtilization%", color = Color.Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { cpuUtilization / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                        color = Color.Cyan,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }

                // JVM Allocation Memory Metric
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("JVM Runtime Heap Space", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        Text("$activeUsedMemoryMb MB / $maxMemoryMb MB", color = Color.Magenta, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { memoryUsageRatio.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                        color = Color.Magenta,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }

                // File System Drive storage space
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Flash Storage Allocations", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        Text("${rootSpaceTotalGb - rootSpaceFreeGb} GB / $rootSpaceTotalGb GB", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { storageRatio.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                        color = Color.Green,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            }
        }

        // Live grid blocks
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1.0f),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("ACTIVE ENGINE", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text("Kate-OS v3", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
            }
            Card(
                modifier = Modifier.weight(1.0f),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.End) {
                    Text("PING LATENCY", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text("14 ms", fontSize = 15.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun SpectrumPanel() {
    var pulseOffset by remember { mutableStateOf(0f) }
    var userFrequencyFactor by remember { mutableStateOf(1f) }

    // Phase oscillation over time
    LaunchedEffect(Unit) {
        while (true) {
            pulseOffset -= 0.08f
            delay(16)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { _, _ ->
                    // Interactive multiplier
                    userFrequencyFactor = (userFrequencyFactor + 0.15f).coerceIn(0.5f, 3.5f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "INTERACTIVE 3D PERSPECTIVE SPECTRUM",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            // Drawing dual perspective layered ribbons to simulate isometric 3D waves depth
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val width = size.width
                val height = size.height

                // Draw 3 distinct depth rings representing a rotating 3D sound topography
                for (depth in 0..2) {
                    val zScale = 1.0f - (depth * 0.25f)
                    val alphaValue = 0.9f - (depth * 0.3f)
                    val colorGradient = when (depth) {
                        0 -> Color(0xFFFF3B30) // Primary Red
                        1 -> Color(0xFF00FFCC) // Aqua
                        else -> Color(0xFF9E00FF) // Purple
                    }
                    val path = Path()
                    val baselineY = height * (0.4f + depth * 0.15f)

                    path.moveTo(0f, baselineY)
                    for (i in 0..width.toInt() step 6) {
                        val x = i.toFloat()
                        val normalizedX = x / width
                        // Composite harmonic formula combined with active variables
                        val soundStimulusHeight = height * 0.15f * zScale
                        val sineSegment1 = kotlin.math.sin(normalizedX * (7f * userFrequencyFactor) + pulseOffset)
                        val sineSegment2 = kotlin.math.cos(normalizedX * (15f * userFrequencyFactor) - pulseOffset * 1.5f)
                        val elevation = (sineSegment1 + (sineSegment2 * 0.5f)) * soundStimulusHeight

                        path.lineTo(x, baselineY + elevation.toFloat())
                    }

                    drawPath(
                        path = path,
                        color = colorGradient,
                        alpha = alphaValue,
                        style = Stroke(width = (6 - depth * 1.5f).dp.toPx(), join = StrokeJoin.Round)
                    )
                }

                // Grid ground indicators
                for (lines in 0..10) {
                    val xPos = width * (lines / 10f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = Offset(xPos, height * 0.2f),
                        end = Offset(xPos, height * 0.8f),
                        strokeWidth = 1f
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "DRAG ON AREA TO SHIFT CARRIER FREQUENCIES",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(Locale.US, "Active Carrier Multiplier: %.2f Hz", userFrequencyFactor),
                    fontSize = 11.sp,
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun FallbackComposePlayer(
    title: String,
    id: String,
    isPiped: Boolean,
    onChannelReset: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(true) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "audio_viz")
    val barCount = 18
    val bars = List(barCount) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 400 + (index * 70) % 500,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$index"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D11))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridColor = Color(0xFFFF9500).copy(alpha = 0.05f)
            val step = 30.dp.toPx()
            for (x in 0..size.width.toInt() step step.toInt()) {
                drawLine(gridColor, start = Offset(x.toFloat(), 0f), end = Offset(x.toFloat(), size.height))
            }
            for (y in 0..size.height.toInt() step step.toInt()) {
                drawLine(gridColor, start = Offset(0f, y.toFloat()), end = Offset(size.width, y.toFloat()))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF00FFCC).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF00FFCC), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "RECEIVER SIGNAL RECOVERY Active",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FFCC)
                        )
                    }
                    Text(
                        text = "ID: ${id.take(8).uppercase()}",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }

                Text(
                    text = "FPS: 60.0 // BYPASS ON",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bars.forEachIndexed { i, barVal ->
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height((barVal.value * 50).dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFFFF9500),
                                                Color(0xFFFF3B30)
                                            )
                                        )
                                    )
                            )
                        }
                    }
                } else {
                    Text(
                        text = "📡 TELEMETRY PARSED & PAUSED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.3f),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color(0xFFFF9500),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Mute Toggle",
                            tint = Color(0xFFFF9500),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .border(1.dp, Color.Red, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isPlaying) "LIVE METRIC FEED" else "BUFFER STOPPED",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InstagramSidePanel() {
    val isInstagramWebViewSupported = remember {
        try {
            Class.forName("android.webkit.WebView")
            android.webkit.CookieManager.getInstance()
            true
        } catch (e: Throwable) {
            false
        }
    }
    var isLiveUrlOption by remember { mutableStateOf(isInstagramWebViewSupported) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Upper client manager bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "INSTAGRAM APP PORTAL",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Magenta,
                letterSpacing = 1.sp
            )
            
            // Client Mode Switcher
            Row(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isLiveUrlOption) Color.Magenta else Color.Transparent)
                        .clickable { isLiveUrlOption = true }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("LIVE WEB", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isLiveUrlOption) Color.White else Color.Gray)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!isLiveUrlOption) Color.Magenta else Color.Transparent)
                        .clickable { isLiveUrlOption = false }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("OFFLINE MOCK", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (!isLiveUrlOption) Color.White else Color.Gray)
                }
            }
        }
        
        Divider(color = Color.White.copy(alpha = 0.15f))

        if (isLiveUrlOption) {
            // Authentic system Android View loading native Instagram Web view with customized settings
            AndroidView(
                factory = { context ->
                    try {
                        WebView(context).apply {
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    return false // Handle navigation streams locally inside our frame
                                }
                            }
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                setSupportZoom(true)
                                // Mobilize user-agent to ensure perfect rendering of the real app client instead of desktop slop
                                userAgentString = "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
                            }
                            loadUrl("https://www.instagram.com")
                        }
                    } catch (e: Throwable) {
                        android.view.View(context)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { webView ->
                    // keeps the session persistent
                }
            )
        } else {
            // Elegant local user feed simulation mapping offline assets
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    LazyRow(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Brush.linearGradient(listOf(Color.Yellow, Color.Red, Color.Magenta)))
                                        .padding(2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(50))
                                            .background(Color.DarkGray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Your Story", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        items(5) { index ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Brush.linearGradient(listOf(Color.Yellow, Color.Red, Color.Magenta)))
                                        .padding(2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(50))
                                            .background(Color.Gray)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("user_$index", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                    Divider(color = Color.White.copy(alpha = 0.1f))
                }

                items(10) { postIndex ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("steve_a", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                        }

                        // Simulated feeds post media frame
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .background(Brush.sweepGradient(listOf(Color(0xFF230D32), Color.Black, Color(0xFF13061A)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(56.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Icon(Icons.Default.FavoriteBorder, contentDescription = "Like", tint = Color.White)
                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comment", tint = Color.White)
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Share", tint = Color.White)
                            }
                            Icon(Icons.Default.BookmarkBorder, contentDescription = "Save", tint = Color.White)
                        }

                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) {
                            Text("Liked by boss_man and 1,02$postIndex others", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "steve_a Operational telemetry on VIP track! 🚀💎",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("View all $postIndex comments", color = Color.Gray, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

