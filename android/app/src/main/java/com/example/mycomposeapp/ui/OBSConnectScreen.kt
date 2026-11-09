package com.example.mycomposeapp.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Lock
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

// ── COLOR PALETTE ──
private val White = Color(0xFFFFFFFF)
private val Black = Color(0xFF000000)
private val Gray = Color(0xFF888888)
private val Green = Color(0xFF00E676)
private val Orange = Color(0xFFFF9100)
private val Red = Color(0xFFFF1744)
private val Blue = Color(0xFF448AFF)
private val Cyan = Color(0xFF00E5FF)
private val Purple = Color(0xFFD500F9)

// ── CONNECTION STATES ──
enum class ConnectionState {
    DISCONNECTED, CONNECTING, AUTHENTICATING, CONNECTED, FEEDING, ERROR
}

// ── STATUS TYPES ──
enum class StatusType {
    OFFLINE, STANDBY, ACTIVE, LOCKED, UNLOCKED, ERROR
}

// ── CONNECTION MANAGER ──
class OBSConnectionManager {
    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    private var currentState = ConnectionState.DISCONNECTED
    private val stateListeners = mutableListOf<(ConnectionState) -> Unit>()
    private val messageListeners = mutableListOf<(String) -> Unit>()
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    fun getState() = currentState

    fun addStateListener(listener: (ConnectionState) -> Unit) {
        stateListeners.add(listener)
    }

    fun addMessageListener(listener: (String) -> Unit) {
        messageListeners.add(listener)
    }

    fun connect(ip: String, port: String, secret: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val host = if (ip.contains(":")) ip else "$ip:$port"
        val url = "ws://$host"

        currentState = ConnectionState.CONNECTING
        notifyState()

        client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url(url).build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                currentState = ConnectionState.AUTHENTICATING
                notifyState()

                // Authenticate with secret
                val authMsg = """{"jsonrpc":"2.0","id":1,"method":"Hello","params":{"Protocols":["obswebsocket"],"Methods":["GetListofSources","GetCurrentScene","GetSourceSettings","SetCurrentScene","SetCurrentSceneItemProperties","GetSceneItemList","GetCurrentProgramSource"]}}"""
                webSocket.send(authMsg)

                // Wait for auth response then send secret
                scope.launch {
                    delay(500)
                    val authComplete = """{"jsonrpc":"2.0","id":2,"method":"Auth","params":{"auth":"$secret"}}"""
                    webSocket.send(authComplete)
                    currentState = ConnectionState.CONNECTED
                    notifyState()
                    onSuccess()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                messageListeners.forEach { it(text) }

                // Detect connection established
                if (text.contains("\"result\":\"ok\"") || text.contains("\"event\"")) {
                    if (currentState == ConnectionState.AUTHENTICATING) {
                        currentState = ConnectionState.CONNECTED
                        notifyState()
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                currentState = ConnectionState.ERROR
                notifyState()
                onError(t.message ?: "Connection failed")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                currentState = ConnectionState.DISCONNECTED
                notifyState()
            }
        }

        webSocket = client?.newCall(request)?.execute()?.let {
            client?.newCall(request)?.enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    currentState = ConnectionState.ERROR
                    notifyState()
                    onError(e.message ?: "Connection failed")
                }

                override fun onResponse(call: okhttp3.Call, response: Response) {
                    if (response.code == 101) {
                        webSocket = response.websocket()
                        webSocket?.addListener(listener)
                    }
                }
            })
        } ?: client?.newCall(request)?.execute()?.apply {
            webSocket = this.websocket()
            webSocket?.addListener(listener)
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Disconnecting")
        client?.dispatcher?.executorService?.shutdown()
        currentState = ConnectionState.DISCONNECTED
        notifyState()
    }

    fun sendFrameData(data: String) {
        if (currentState == ConnectionState.CONNECTED || currentState == ConnectionState.FEEDING) {
            val msg = """{"jsonrpc":"2.0","id":3,"method":"SendFrame","params":{"image":"$data"}}"""
            webSocket?.send(msg)
        }
    }

    fun startFeeding() {
        currentState = ConnectionState.FEEDING
        notifyState()
    }

    fun stopFeeding() {
        if (currentState == ConnectionState.FEEDING) {
            currentState = ConnectionState.CONNECTED
            notifyState()
        }
    }

    private fun notifyState() {
        stateListeners.forEach { it(currentState) }
    }

    fun cleanup() {
        scope.cancel()
        client?.dispatcher?.executorService?.shutdown()
    }
}

// ── ANIMATED DOT COMPONENT ──
@Composable
private fun AnimatedDot(
    progress: Float,
    startX: Float,
    endX: Float,
    startY: Float,
    endY: Float,
    dotColor: Color,
    trailColor: Color,
    size: Float = 6f,
    trailLength: Float = 40f
) {
    val currentX = startX + (endX - startX) * progress
    val currentY = startY + (endY - startY) * progress

    Canvas(modifier = Modifier.size(50.dp)) {
        // Draw trail (fading line behind the dot)
        if (progress > 0f) {
            val trailStartX = maxOf(startX, currentX - trailLength)
            val trailStartY = startY + (endY - startY) * (trailStartX - startX) / (endX - startX)
            drawLine(
                color = trailColor.copy(alpha = 0.3f),
                start = Offset(trailStartX, trailStartY),
                end = Offset(currentX, currentY),
                strokeWidth = 2f
            )
        }

        // Draw the main dot
        drawCircle(
            color = dotColor,
            radius = size,
            center = Offset(currentX, currentY)
        )
    }
}

// ── TRANSMITTER PIPELINE ANIMATION ──
@Composable
private fun TransmitterPipeline(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var dots by remember { mutableStateOf<List<Pair<Float, Color>>>(emptyList()) }

    // Animated dots flowing through the pipeline
    LaunchedEffect(isActive) {
        if (!isActive) {
            dots = emptyList()
            return@LaunchedEffect
        }

        // Generate dots at intervals
        val dotColors = listOf(Green, Cyan, Blue, Purple, Orange)
        while (isActive) {
            val newDots = (0..2).map { i ->
                Pair(0f, dotColors[i % dotColors.size])
            }
            dots = newDots

            for (dot in newDots) {
                val idx = newDots.indexOf(dot)
                scope.launch {
                    for (p in 0f..1f step 0.03f) {
                        dots = dots.mapIndexed { i, (prog, col) ->
                            if (i == idx) Pair(p.coerceIn(0f, 1f), col) else (prog, col)
                        }
                        delay(16)
                    }
                }
            }
            delay(400)
        }
    }

    // Draw the pipeline with animated dots
    Canvas(modifier = modifier.fillMaxWidth()) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        // Draw the pipeline line
        drawLine(
            color = if (isActive) Green.copy(alpha = 0.4f) else Gray.copy(alpha = 0.2f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1.5f
        )

        // Draw animated dots
        for ((progress, color) in dots) {
            val x = progress * width
            drawCircle(
                color = color,
                radius = 4f,
                center = Offset(x, centerY)
            )

            // Draw glow effect
            drawCircle(
                color = color.copy(alpha = 0.3f),
                radius = 8f,
                center = Offset(x, centerY)
            )
        }

        // Draw connection points at each section
        val sectionPositions = listOf(0.1f, 0.3f, 0.5f, 0.7f, 0.9f)
        for (pos in sectionPositions) {
            val x = pos * width
            drawCircle(
                color = if (isActive) Green.copy(alpha = 0.6f) else Gray.copy(alpha = 0.3f),
                radius = 3f,
                center = Offset(x, centerY)
            )
        }
    }
}

// ── STATUS INDICATOR WITH DYNAMIC STATE ──
@Composable
private fun DynamicStatusIndicator(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    status: StatusType,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val (statusText, statusColor) = when (status) {
        StatusType.OFFLINE -> "OFFLINE" to Gray
        StatusType.STANDBY -> "STANDBY" to Orange
        StatusType.ACTIVE -> "ACTIVE" to Green
        StatusType.LOCKED -> "LOCKED" to Orange
        StatusType.UNLOCKED -> "UNLOCKED" to Green
        StatusType.ERROR -> "ERROR" to Red
    }

    Card(
        modifier = modifier
            .width(100.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, if (isActive) statusColor.copy(alpha = 0.5f) else Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = Black),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) statusColor else Gray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                color = White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = statusText,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) statusColor else Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── MAIN OBS CONNECT SCREEN ──
@Composable
fun OBSConnectScreen(
    onBack: () -> Unit,
    onCardClick: () -> Unit,
    onConnected: () -> Unit = {}
) {
    var serverIp by remember { mutableStateOf("127.0.0.1") }
    var port by remember { mutableStateOf("4455") }
    var authSecret by remember { mutableStateOf("") }

    var connectionState by remember { mutableStateOf(ConnectionState.DISCONNECTED) }
    var obsFeedStatus by remember { mutableStateOf(StatusType.OFFLINE) }
    var virtualCamStatus by remember { mutableStateOf(StatusType.STANDBY) }
    var appSelectorStatus by remember { mutableStateOf(StatusType.LOCKED) }
    var bridgeLocked by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isFeeding by remember { mutableStateOf(false) }

    val connectionManager = remember { OBSConnectionManager() }

    // Listen for state changes
    remember {
        connectionManager.addStateListener { state ->
            connectionState = state
            when (state) {
                ConnectionState.CONNECTING -> {
                    obsFeedStatus = StatusType.STANDBY
                    virtualCamStatus = StatusType.STANDBY
                    appSelectorStatus = StatusType.LOCKED
                    bridgeLocked = true
                }
                ConnectionState.CONNECTED -> {
                    obsFeedStatus = StatusType.ACTIVE
                    virtualCamStatus = StatusType.ACTIVE
                    bridgeLocked = false
                }
                ConnectionState.FEEDING -> {
                    isFeeding = true
                    appSelectorStatus = StatusType.UNLOCKED
                }
                ConnectionState.ERROR -> {
                    // error state
                }
                ConnectionState.AUTHENTICATING -> {
                    obsFeedStatus = StatusType.STANDBY
                    virtualCamStatus = StatusType.STANDBY
                }
                ConnectionState.DISCONNECTED -> {
                    obsFeedStatus = StatusType.OFFLINE
                    virtualCamStatus = StatusType.STANDBY
                    appSelectorStatus = StatusType.LOCKED
                    bridgeLocked = true
                    isFeeding = false
                }
            }
        }
    }

    // Cleanup on dispose
    remember {
        connectionManager.addMessageListener { _ -> }
    }

    DisposableEffect(Unit) {
        onDispose {
            connectionManager.cleanup()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = White
                    )
                }
                Text(
                    text = "OBS CONNECT",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
                // Connection status badge
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            when (connectionState) {
                                ConnectionState.CONNECTED, ConnectionState.FEEDING -> Green
                                ConnectionState.CONNECTING, ConnectionState.AUTHENTICATING -> Orange
                                ConnectionState.ERROR -> Red
                                else -> Gray
                            }
                        )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // A. WebSocket Server Configuration
            Text(
                text = "WebSocket Server Config",
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
                    // Server IP Address
                    FormField(
                        label = "Server IP Address",
                        placeholder = "127.0.0.1",
                        value = serverIp,
                        onValueChange = { serverIp = it },
                        onClick = onCardClick
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Port
                    FormField(
                        label = "Port",
                        placeholder = "4455",
                        value = port,
                        onValueChange = { port = it },
                        onClick = onCardClick
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Authentication Secret
                    FormField(
                        label = "Authentication Secret",
                        placeholder = "Enter authentication secret",
                        value = authSecret,
                        onValueChange = { authSecret = it },
                        onClick = onCardClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // B. ESTABLISH CONNECTION BUTTON
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, White, RoundedCornerShape(12.dp))
                    .clickable(onClick = {
                        if (connectionState == ConnectionState.DISCONNECTED || connectionState == ConnectionState.ERROR) {
                            connectionManager.connect(
                                ip = serverIp,
                                port = port,
                                secret = authSecret,
                                onSuccess = {
                                    connectionManager.startFeeding()
                                    onConnected()
                                },
                                onError = { msg ->
                                    errorMessage = msg
                                }
                            )
                        } else if (connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.FEEDING) {
                            connectionManager.disconnect()
                        }
                    }, enabled = connectionState != ConnectionState.CONNECTING),
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
                    AnimatedContent(
                        targetState = connectionState,
                        transitionSpec = {
                            fadeIn() + slideInVertically { it / 2 } with
                            fadeOut() + slideOutVertically { -it / 2 }
                        }
                    ) { state ->
                        when (state) {
                            ConnectionState.CONNECTING, ConnectionState.AUTHENTICATING -> {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Connecting",
                                    tint = Orange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "CONNECTING...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Orange,
                                    textAlign = TextAlign.Center
                                )
                            }
                            ConnectionState.CONNECTED, ConnectionState.FEEDING -> {
                                Icon(
                                    imageVector = Icons.Default.LinkOff,
                                    contentDescription = "Disconnect",
                                    tint = Green,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DISCONNECT",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Green,
                                    textAlign = TextAlign.Center
                                )
                            }
                            ConnectionState.ERROR -> {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = Red,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "RECONNECT",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Red,
                                    textAlign = TextAlign.Center
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = "Connect",
                                    tint = White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ESTABLISH CONNECTION",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Error message
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Red.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Error: $errorMessage",
                        fontSize = 11.sp,
                        color = Red,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // C. TRANSMITTER PIPELINE ANIMATION
            Text(
                text = "Transmitter Pipeline",
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
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .height(60.dp)
                ) {
                    // Pipeline visualization with animated dots
                    TransmitterPipeline(
                        isActive = connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.FEEDING,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Section labels below the pipeline
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Virt Cam",
                            fontSize = 8.sp,
                            color = if (virtualCamStatus == StatusType.ACTIVE) Green else Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Bridge",
                            fontSize = 8.sp,
                            color = if (connectionState == ConnectionState.CONNECTED) Green else Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "OBS",
                            fontSize = 8.sp,
                            color = if (obsFeedStatus == StatusType.ACTIVE) Green else Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // D. BRIDGE ROUTING CONTROLLER
            Text(
                text = "Bridge Routing Controller",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = White,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp,
                        if (!bridgeLocked) Green.copy(alpha = 0.5f) else Gray.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (!bridgeLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = "Bridge Status",
                            tint = if (!bridgeLocked) Green else Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (bridgeLocked) "Bridge Locked" else "Bridge Active",
                            fontSize = 12.sp,
                            color = if (!bridgeLocked) Green else Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Animated bridge status indicator
                    if (!bridgeLocked) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Green)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // E. BRIDGE STATUS INDICATORS
            Text(
                text = "Bridge Status",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = White,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DynamicStatusIndicator(
                    icon = Icons.Default.Monitor,
                    label = "OBS Feed",
                    status = obsFeedStatus,
                    isActive = connectionState != ConnectionState.DISCONNECTED
                )
                DynamicStatusIndicator(
                    icon = Icons.Default.Videocam,
                    label = "Virtual Cam",
                    status = virtualCamStatus,
                    isActive = connectionState != ConnectionState.DISCONNECTED
                )
                DynamicStatusIndicator(
                    icon = Icons.Default.Lock,
                    label = "App Selector",
                    status = appSelectorStatus,
                    isActive = connectionState != ConnectionState.DISCONNECTED
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // F. CONNECTION ACTION BUTTON
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp,
                        if (connectionState == ConnectionState.FEEDING) Green.copy(alpha = 0.5f) else Gray.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    ),
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
                    AnimatedContent(
                        targetState = connectionState,
                        transitionSpec = {
                            fadeIn() + slideInVertically { it / 2 } with
                            fadeOut() + slideOutVertically { -it / 2 }
                        }
                    ) { state ->
                        when (state) {
                            ConnectionState.FEEDING -> {
                                Icon(
                                    imageVector = Icons.Default.Selfie,
                                    contentDescription = "Feeding",
                                    tint = Green,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "FEEDING TO TARGET APP",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Green,
                                    textAlign = TextAlign.Center
                                )
                            }
                            ConnectionState.CONNECTED -> {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start Feeding",
                                    tint = White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "CONNECTED - READY TO FEED",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White,
                                    textAlign = TextAlign.Center
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Connect",
                                    tint = Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Connect OBS to run flow",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FormField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = White,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .border(1.dp, White, RoundedCornerShape(8.dp))
                .clickable(onClick = onClick),
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
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 12.sp,
                            color = Gray
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}
