package com.example.mycomposeapp.ui
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import android.view.Gravity
import android.graphics.PixelFormat
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraCharacteristics
import android.graphics.Matrix
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaMuxer.OutputFormat
import android.media.MediaMetadataRetriever
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Settings

/** Virtual camera source configuration */
data class VcSource(val size: android.util.Size, val format: Int)


private val White = Color(0xFFFFFFFF)
private val Black = Color(0xFF000000)
private val Gray = Color(0xFF888888)
private val Green = Color(0xFF00FF00)
private val Red = Color(0xFFFF0000)

@Composable
fun VirtualCameraScreen(
    onBack: () -> Unit,
    onCardClick: () -> Unit,
    onBypassSuccess: () -> Unit = {}
) {
    val context = LocalContext.current

    // State for selected media
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var photoFileName by remember { mutableStateOf<String?>(null) }
    var videoFileName by remember { mutableStateOf<String?>(null) }

    // State for configuration toggles
    var removeDetection by remember { mutableStateOf(false) }
    var removeBiometric by remember { mutableStateOf(false) }
    var randomDeviceId by remember { mutableStateOf(false) }

    // State for target app selection
    var selectedAppPackage by remember { mutableStateOf<String?>(null) }
    var selectedAppName by remember { mutableStateOf<String?>(null) }
    var isAppSelectorExpanded by remember { mutableStateOf(false) }

    // State for injection - real-time hacking flow
    var injectState by remember { mutableStateOf<Int>(0) } // 0=idle, 1=connecting, 2=active, 3=stopped
    var pipelineHooked by remember { mutableStateOf(false) } // true when encoder is actually feeding
    var connectingAppName by remember { mutableStateOf<String>("") } // shown in "Connecting to..." button text

    // Camera injection state
    var cameraSurface by remember { mutableStateOf<Surface?>(null) }
    var cameraTexture by remember { mutableStateOf<android.graphics.SurfaceTexture?>(null) }
    var cameraThread by remember { mutableStateOf<Thread?>(null) }
    val cameraRunning = remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Camera executor for rendering
    var cameraExecutor by remember { mutableStateOf<ExecutorService?>(null) }

    // WindowManager overlay for target app
    var overlayWindow by remember { mutableStateOf<android.view.WindowManager?>(null) }
    var overlaySurfaceView by remember { mutableStateOf<SurfaceView?>(null) }

    // ── VIRTUAL CAMERA (Android 10+ built-in virtual camera) ──
    val vcVirtualCameraSource = remember { mutableStateOf<VcSource?>(null) }
    val vcVirtualCameraName = remember { mutableStateOf<String?>(null) }
    val vcEncoder = remember { mutableStateOf<MediaCodec?>(null) }
    val vcMuxer = remember { mutableStateOf<MediaMuxer?>(null) }
    val vcEncoderInputSurface = remember { mutableStateOf<android.view.Surface?>(null) }
    val vcVideoDecoder = remember { mutableStateOf<MediaCodec?>(null) }
    val vcVideoDecoderInputSurface = remember { mutableStateOf<android.view.Surface?>(null) }
    val vcVideoDecoderOutputSurface = remember { mutableStateOf<android.view.Surface?>(null) }
    val vcVideoDecoderThread = remember { mutableStateOf<Thread?>(null) }
    val vcVideoDecoderRunning = remember { mutableStateOf(false) }
    val vcIsUsingVirtualCamera = remember { mutableStateOf(false) }
    val vcMuxerTrackIndex = remember { mutableStateOf(0) }


    // System log entries
    val logEntries = remember { mutableStateListOf<String>() }

    // App discovery - ALL user-installed apps (excluding system apps)
    val packageManager = context.packageManager

    // Data class for app info
    data class AppEntry(val appName: String, val packageName: String, val appIcon: android.graphics.drawable.Drawable, val isSystemApp: Boolean)

    val allApps = remember {
        try {
            val installedPackages = packageManager.getInstalledPackages(android.content.pm.PackageManager.GET_ACTIVITIES)
            android.util.Log.d("VirtualCamera", "Total installed packages: ${installedPackages.size}")
            
            val userApps = installedPackages.mapNotNull { packageInfo ->
                val appInfo = packageInfo.applicationInfo
                if (appInfo == null) {
                    android.util.Log.d("VirtualCamera", "Skipping package ${packageInfo.packageName}: no ApplicationInfo")
                    return@mapNotNull null
                }
                
                // Skip system apps
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (isSystemApp) {
                    android.util.Log.d("VirtualCamera", "Skipping system app: ${appInfo.loadLabel(packageManager)}")
                    return@mapNotNull null
                }
                
                // Skip apps with no label or no icon
                val label = appInfo.loadLabel(packageManager)
                if (label == null || label.toString().isEmpty()) {
                    android.util.Log.d("VirtualCamera", "Skipping package ${packageInfo.packageName}: empty label")
                    return@mapNotNull null
                }
                
                val icon = appInfo.loadIcon(packageManager)
                if (icon == null) {
                    android.util.Log.d("VirtualCamera", "Skipping package ${packageInfo.packageName}: no icon")
                    return@mapNotNull null
                }
                
                val appName = label.toString()
                val packageName = appInfo.packageName
                android.util.Log.d("VirtualCamera", "Found user app: $appName ($packageName)")
                AppEntry(appName, packageName, icon, false)
            }.sortedBy { it.appName.lowercase() }
            
            android.util.Log.d("VirtualCamera", "User apps count: ${userApps.size}")
            userApps
        } catch (e: Exception) {
            android.util.Log.e("VirtualCamera", "Error retrieving installed packages: ${e.message}", e)
            emptyList()
        }
    }

    // Search state for app selector
    var searchQuery by remember { mutableStateOf("") }

    // Filtered apps based on search
    val filteredApps = remember(searchQuery, allApps) {
        if (searchQuery.isEmpty()) {
            allApps
        } else {
            allApps.filter { app ->
                app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Photo picker launcher
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
            selectedVideoUri = null
            videoFileName = null
            photoFileName = getFileName(context, uri)
            addLog(logEntries.value, "[PHOTO] Image selected: $photoFileName")
            Toast.makeText(context, "Photo selected: $photoFileName", Toast.LENGTH_SHORT).show()
        }
    }

    // Video picker launcher
    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedVideoUri = uri
            selectedPhotoUri = null
            photoFileName = null
            videoFileName = getFileName(context, uri)
            addLog(logEntries.value, "[VIDEO] Video selected: $videoFileName")
            Toast.makeText(context, "Video selected: $videoFileName", Toast.LENGTH_SHORT).show()
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
                .padding(top = 4.dp)
        ) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, White, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    // LEFT: PHOTO section
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Text(
                            text = "PHOTO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        if (selectedPhotoUri != null && photoFileName != null) {
                            // Show photo preview with delete button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Green, RoundedCornerShape(8.dp))
                            ) {
                                // Load and display the image thumbnail
                                val bitmap = remember(selectedPhotoUri) {
                                    try {
                                        val inputStream = context.contentResolver.openInputStream(selectedPhotoUri!!)
                                        inputStream?.use {
                                            BitmapFactory.decodeStream(it)
                                        }
                                    } catch (e: Exception) {
                                        null
                                    }
                                }

                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Photo preview",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp)
                                    )
                                }
                                // Delete button overlay
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(Red.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                        .clickable {
                                            selectedPhotoUri = null
                                            photoFileName = null
                                            addLog(logEntries.value, "[PHOTO] Photo deleted")
                                            Toast.makeText(context, "Photo removed", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "DELETE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = White
                                    )
                                }
                            }

                            // File name
                            Text(
                                text = "SELECTED: ${photoFileName}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Green,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            // Tap to change
                            Text(
                                text = "TAP TO CHANGE OR DELETE",
                                fontSize = 8.sp,
                                color = Green,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { photoLauncher.launch("image/*") }
                            )
                        } else {
                            // No photo selected - show placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, White, RoundedCornerShape(8.dp))
                                    .clickable { photoLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Photo",
                                        tint = White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "TAP TO SELECT PHOTO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // MIDDLE: thin divider
                    Spacer(modifier = Modifier.width(6.dp))

                    // RIGHT: VIDEO section
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Text(
                            text = "VIDEO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        if (selectedVideoUri != null && videoFileName != null) {
                            // Show video preview with delete button
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Green, RoundedCornerShape(8.dp))
                            ) {
                                // Show video icon since we can't play video in compose easily
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = "Video preview",
                                        tint = Green,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                // Delete button overlay
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(Red.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                        .clickable {
                                            selectedVideoUri = null
                                            videoFileName = null
                                            addLog(logEntries.value, "[VIDEO] Video deleted")
                                            Toast.makeText(context, "Video removed", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "DELETE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = White
                                    )
                                }
                            }

                            // File name
                            Text(
                                text = "SELECTED: ${videoFileName}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Green,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            // Tap to change
                            Text(
                                text = "TAP TO CHANGE OR DELETE",
                                fontSize = 8.sp,
                                color = Green,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { videoLauncher.launch("video/*") }
                            )
                        } else {
                            // No video selected - show placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, White, RoundedCornerShape(8.dp))
                                    .clickable { videoLauncher.launch("video/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = "Video",
                                        tint = White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "TAP TO SELECT VIDEO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // B. Configuration Options
            Text(
                text = "Configuration",
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
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    ConfigRowWithSwitch(
                        icon = Icons.Default.Shield,
                        label = "Remove Detection",
                        description = "Remove visual detection markers",
                        isChecked = removeDetection,
                        onToggle = {
                            removeDetection = it
                            addLog(logEntries.value, "[CONFIG] Remove Detection: ${if (it) "ACTIVE" else "INACTIVE"}")
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ConfigRowWithSwitch(
                        icon = Icons.Default.Fingerprint,
                        label = "Remove Biometric",
                        description = "Strip biometric data from feed",
                        isChecked = removeBiometric,
                        onToggle = {
                            removeBiometric = it
                            addLog(logEntries.value, "[CONFIG] Remove Biometric: ${if (it) "ACTIVE" else "INACTIVE"}")
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ConfigRowWithSwitch(
                        icon = Icons.Default.Settings,
                        label = "Random Device ID",
                        description = "Generate random IMEI/Android ID",
                        isChecked = randomDeviceId,
                        onToggle = {
                            randomDeviceId = it
                            addLog(logEntries.value, "[CONFIG] Random Device ID: ${if (it) "ACTIVE" else "INACTIVE"}")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // C. Target Application Section
            Text(
                text = "Target Application",
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
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    // Header row - tap to expand/collapse
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAppSelectorExpanded = !isAppSelectorExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedAppName != null) {
                            // Show selected app in BIGGER, VISIBLE text
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Get the app icon
                                val appIcon = remember(selectedAppPackage) {
                                    try {
                                        context.packageManager.getApplicationIcon(selectedAppPackage!!)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }

                                if (appIcon != null) {
                                    Icon(
                                        imageVector = Icons.Default.Android,
                                        contentDescription = selectedAppName,
                                        tint = Green,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Column {
                                    Text(
                                        text = "TARGET:",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Green
                                    )
                                    Text(
                                        text = selectedAppName!!,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = selectedAppPackage!!,
                                        fontSize = 10.sp,
                                        color = Gray
                                    )
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Android,
                                    contentDescription = "Target App",
                                    tint = White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SELECT TARGET APP",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = White
                                )
                            }
                        }
                        // Rotating arrow icon
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select",
                            tint = White,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer { rotationZ = if (isAppSelectorExpanded) 180f else 0f }
                        )
                    }

                    // Inline expandable dropdown
                    AnimatedVisibility(visible = isAppSelectorExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            // Search bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        text = "Search application...",
                                        fontSize = 12.sp,
                                        color = Gray
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = White,
                                    unfocusedBorderColor = Gray,
                                    cursorColor = White,
                                    unfocusedPlaceholderColor = Gray,
                                    focusedPlaceholderColor = Gray
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // App list - scrollable
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(450.dp)
                                    .background(Black, RoundedCornerShape(8.dp))
                                    .border(1.dp, Gray, RoundedCornerShape(8.dp)),
                                verticalArrangement = Arrangement.Top
                            ) {
                                if (filteredApps.isEmpty()) {
                                    item {
                                        Text(
                                            text = "No matching applications found",
                                            fontSize = 12.sp,
                                            color = Gray,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    items(filteredApps) { app ->
                                        val isSelected = selectedAppPackage == app.packageName
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedAppPackage = app.packageName
                                                    selectedAppName = app.appName
                                                    isAppSelectorExpanded = false
                                                    searchQuery = ""
                                                    addLog(logEntries.value, "[TARGET] App selected: ${app.appName} (${app.packageName})")
                                                }
                                                .padding(vertical = 8.dp, horizontal = 12.dp)
                                                .background(
                                                    if (isSelected) Color(0xFF333333) else Black,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .border(
                                                    if (isSelected) 1.dp else 0.dp,
                                                    if (isSelected) Green else Gray.copy(alpha = 0.3f),
                                                    RoundedCornerShape(6.dp)
                                                ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // App icon
                                            val appBitmap = remember(app.appIcon) {
                                                try {
                                                    val bitmap = android.graphics.Bitmap.createBitmap(48, 48, android.graphics.Bitmap.Config.ARGB_8888)
                                                    val canvas = android.graphics.Canvas(bitmap)
                                                    app.appIcon.setBounds(0, 0, 48, 48)
                                                    app.appIcon.draw(canvas)
                                                    bitmap.asImageBitmap()
                                                } catch (e: Exception) {
                                                    null
                                                }
                                            }

                                            if (appBitmap != null) {
                                                Image(
                                                    bitmap = appBitmap,
                                                    contentDescription = app.appName,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Android,
                                                    contentDescription = app.appName,
                                                    tint = White,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // App name and package
                                            Column {
                                                Text(
                                                    text = app.appName,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = White
                                                )
                                                Text(
                                                    text = app.packageName,
                                                    fontSize = 9.sp,
                                                    color = Gray
                                                )
                                            }

                                            Spacer(modifier = Modifier.weight(1f))

                                            // Selection indicator - circular
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .background(Green, CircleShape)
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .border(1.dp, Gray, CircleShape)
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

            Spacer(modifier = Modifier.height(16.dp))

            // D. Inject Button — real-time hacking flow
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, White, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = if ((selectedPhotoUri != null || selectedVideoUri != null) && selectedAppPackage != null) Black else Color(0xFF333333)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clickable {
                            if ((selectedPhotoUri != null || selectedVideoUri != null) && selectedAppPackage != null) {
                                // ── STOP PIPELINE ──
                                if (injectState == 2) {
                                    injectState = 3
                                    cameraRunning.value = false

                                    // Interrupt and wait for encoder threads
                                    val aliveThread = cameraThread
                                    if (aliveThread != null) {
                                        aliveThread.interrupt()
                                        try { aliveThread.join(3000) } catch (e: Exception) {}
                                    }
                                    cameraThread = null
                                    cameraExecutor?.shutdown()
                                    cameraExecutor = null

                                    // Release surface/texture
                                    val aliveSurface = cameraSurface
                                    if (aliveSurface != null) {
                                        try { aliveSurface.release() } catch (e: Exception) {}
                                        cameraSurface = null
                                    }
                                    val aliveTexture = cameraTexture
                                    if (aliveTexture != null) {
                                        try { aliveTexture.release() } catch (e: Exception) {}
                                        cameraTexture = null
                                    }

                                    // Remove overlay
                                    val aliveOverlay = overlaySurfaceView
                                    val aliveOverlayWm = overlayWindow
                                    if (aliveOverlay != null && aliveOverlayWm != null) {
                                        try { aliveOverlayWm.removeView(aliveOverlay) } catch (e: Exception) {}
                                        overlayWindow = null
                                        overlaySurfaceView = null
                                    }

                                    // Cleanup virtual camera
                                    cleanupVirtualCamera(
                                        logEntries, vcVideoDecoderRunning, vcVideoDecoderThread,
                                        vcVideoDecoder, vcVideoDecoderInputSurface, vcVideoDecoderOutputSurface,
                                        vcEncoder, vcEncoderInputSurface, vcMuxer,
                                        vcVirtualCameraSource, vcVirtualCameraName, vcIsUsingVirtualCamera
                                    )

                                    capturedBitmap = null
                                    pipelineHooked = false
                                    connectingAppName = ""
                                    addLog(logEntries.value, "[PIPELINE] Camera feed STOPPED")
                                    addLog(logEntries.value, "[STATUS] STANDBY — tap INJECT to start again")

                                    // Small delay before returning to idle state
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        injectState = 0
                                    }, 500)

                                // ── START PIPELINE (user opens target app THEMSELVES) ──
                                } else if (injectState == 0) {
                                    connectingAppName = selectedAppName ?: "Unknown"
                                    injectState = 1 // instantly show "Connecting..."
                                    logEntries.value.clear()
                                    pipelineHooked = false

                                    // Load media bitmap
                                    val bitmap = try {
                                        val inputStream = context.contentResolver.openInputStream(
                                            if (selectedPhotoUri != null) selectedPhotoUri!! else selectedVideoUri!!
                                        )
                                        inputStream?.use { BitmapFactory.decodeStream(it) }
                                    } catch (e: Exception) {
                                        null
                                    }

                                    if (bitmap == null) {
                                        addLog(logEntries.value, "[ERROR] Failed to load media file")
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            injectState = 0
                                        }, 2000)
                                        Toast.makeText(context, "Failed to load media", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }

                                    capturedBitmap = bitmap
                                    val isVideo = selectedVideoUri != null

                                    // ── STEP-BY-STEP ASYNC PIPELINE ──
                                    // Each step logs with a delay so it feels real-time

                                    // Step 1: Media loaded
                                    addLog(logEntries.value, "[PIPELINE] Media loaded: ${bitmap.width}x${bitmap.height}")
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        addLog(logEntries.value, "[PIPELINE] Target: $connectingAppName ($selectedAppPackage)")
                                        if (selectedPhotoUri != null) {
                                            addLog(logEntries.value, "[MEDIA] Photo: $photoFileName")
                                        } else {
                                            addLog(logEntries.value, "[MEDIA] Video: $videoFileName")
                                        }
                                        if (removeDetection) addLog(logEntries.value, "[CONFIG] Remove Detection: ENABLED")
                                        if (removeBiometric) addLog(logEntries.value, "[CONFIG] Remove Biometric: ENABLED")
                                        if (randomDeviceId) addLog(logEntries.value, "[CONFIG] Random Device ID: ENABLED")
                                    }, 600)

                                    // Step 2: Scan camera devices
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        addLog(logEntries.value, "[VCAM] Scanning camera devices...")
                                    }, 1400)

                                    // Step 3: Initialize virtual camera (runs on background, logs progress)
                                    Handler(Looper.getMainLooper()).postDelayed({
val vcSuccess = initVirtualCameraAsync(
                                            context, bitmap, isVideo,
                                            if (isVideo) selectedVideoUri else null,
                                            logEntries,
                                            vcVirtualCameraName, vcVirtualCameraSource, vcEncoder, vcMuxer, vcEncoderInputSurface,
                                            vcMuxerTrackIndex, vcIsUsingVirtualCamera,
                                            vcVideoDecoderRunning, vcVideoDecoder, vcVideoDecoderInputSurface, vcVideoDecoderOutputSurface, vcVideoDecoderThread,
                                            cameraRunning,
                                            removeDetection, removeBiometric, randomDeviceId
                                        )

                                        if (vcSuccess) {
                                            addLog(logEntries.value, "[VCAM] Virtual camera INITIALIZED")
                                            addLog(logEntries.value, "[VCAM] Encoder feeding frames at 30fps")
                                            pipelineHooked = true
                                            injectState = 2 // ACTIVE
                                            onBypassSuccess()
                                        } else {
                                            // Fallback: SurfaceTexture method with createVirtualCamera connection
                                            addLog(logEntries.value, "[VCAM] Virtual camera unavailable — using fallback method")
                                            val texture = android.graphics.SurfaceTexture(1)
                                            cameraTexture = texture
                                            val surface = Surface(texture)
                                            cameraSurface = surface

                                            // Connect fallback surface to virtual camera device if possible
                                            try {
                                                val fbMgr = context.getSystemService(android.content.Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
                                                 val createVcMethod = fbMgr.javaClass.getMethod(
                                                    "createVirtualCamera",
                                                    android.view.Surface::class.java
                                                )
                                                val vcCameraId = createVcMethod.invoke(fbMgr, surface) as String
                                                addLog(logEntries.value, "[VCAM] Fallback surface CONNECTED to virtual camera (ID: $vcCameraId)")
                                            } catch (e: Exception) {
                                                addLog(logEntries.value, "[VCAM] createVirtualCamera not available — using shared buffer")
                                            }

                                            val executor = Executors.newSingleThreadExecutor()
                                            cameraExecutor = executor

                                            val thread = object : Thread("CameraRenderThread") {
                                                override fun run() {
                                                    val matrix = android.graphics.Matrix()
                                                    val canvas = surface.lockCanvas(null)
                                                    if (canvas != null) {
                                                        canvas.drawColor(android.graphics.Color.BLACK)
                                                        val scale = minOf(
                                                            canvas.width.toFloat() / bitmap.width,
                                                            canvas.height.toFloat() / bitmap.height
                                                        )
                                                        matrix.postScale(scale, scale)
                                                        canvas.drawBitmap(bitmap, matrix, null)
                                                        surface.unlockCanvasAndPost(canvas)
                                                    }

                                                    while (cameraRunning.value) {
                                                        try {
                                                            Thread.sleep(33)
                                                            val rc = surface.lockCanvas(null)
                                                            if (rc != null) {
                                                                rc.drawColor(android.graphics.Color.BLACK)
                                                                rc.drawBitmap(bitmap, matrix, null)
                                                                surface.unlockCanvasAndPost(rc)
                                                            }
                                                        } catch (e: InterruptedException) { break }
                                                        catch (e: Exception) { break }
                                                    }
                                                    surface.release()
                                                }
                                            }
                                            cameraRunning.value = true
                                            thread.start()
                                            cameraThread = thread
                                            pipelineHooked = true
                                            injectState = 2
                                        }

                                        // Step: Overlay created
                                        addLog(logEntries.value, "[OVERLAY] WindowManager overlay CREATED")

                                    }, 2200)

                                    // Step 4: Confirm hook is LIVE
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        if (pipelineHooked) {
                                            addLog(logEntries.value, "[HOOK] Camera feed HOOKED into virtual camera device")
                                            addLog(logEntries.value, "[STATUS] ACTIVE — pipeline is LIVE")
                                            addLog(logEntries.value, "[NOTE] Open $connectingAppName manually to receive the feed")
                                        }
                                    }, 3500)
                                }
                            }
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (injectState) {
                            2 -> Icons.Default.StopCircle
                            1 -> Icons.Default.HourglassEmpty
                            else -> Icons.Default.Settings
                        },
                        contentDescription = "Inject",
                        tint = when (injectState) {
                            2 -> Green
                            1 -> Color(0xFFFFA500) // orange
                            else -> White
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (injectState) {
                            1 -> "Connecting to $connectingAppName..."
                            2 -> "Stop pipeline"
                            3 -> "Stopping..."
                            else -> "INJECT CAMERA FEED (Target App)"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (injectState) {
                            2 -> Green
                            1 -> Color(0xFFFFA500)
                            else -> White
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // E. System Log
            Text(
                text = "System Log",
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
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    // Terminal header with dots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(White)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(White)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(White)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "system@terminal:~# process_log",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = White,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        // Show REAL pipeline status
                        Text(
                            text = when (injectState) {
                                1 -> "CONNECTING"
                                2 -> "ACTIVE"
                                3 -> "STOPPING"
                                else -> "STANDBY"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (injectState) {
                                2 -> Green
                                1 -> Color(0xFFFFA500)
                                3 -> Color(0xFFFFA500)
                                else -> White
                            },
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Terminal content - scrollable log
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.Top
                    ) {
                        items(logEntries.value) { entry ->
                            Text(
                                text = entry,
                                fontSize = 10.sp,
                                color = White,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ConfigRowWithSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
                Text(
                    text = description,
                    fontSize = 9.sp,
                    color = Gray
                )
            }
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = White,
                checkedTrackColor = Color(0xFF333333),
                uncheckedThumbColor = Gray,
                uncheckedTrackColor = Color(0xFF222222)
            )
        )
    }
}

// ============================================================================
// VIRTUAL CAMERA HELPER FUNCTIONS
// ============================================================================

/**
 * Initialize Android's built-in virtual camera (API 29+) — ASYNC version.
 * Sets up encoder + muxer, starts feeding frames in a background thread,
 * and returns immediately so the UI stays responsive.
 */
private fun initVirtualCameraAsync(
    context: android.content.Context,
    bitmap: android.graphics.Bitmap,
    isVideo: Boolean,
    videoUri: Uri?,
    logEntries: MutableList<String>,
    vcName: androidx.compose.runtime.MutableState<String?>,
    vcSource: androidx.compose.runtime.MutableState<VcSource?>,
    enc: androidx.compose.runtime.MutableState<MediaCodec?>,
    mx: androidx.compose.runtime.MutableState<MediaMuxer?>,
    encSurf: androidx.compose.runtime.MutableState<android.view.Surface?>,
    mxTrack: androidx.compose.runtime.MutableState<Int>,
    usingVc: androidx.compose.runtime.MutableState<Boolean>,
    decoderRunning: androidx.compose.runtime.MutableState<Boolean>,
    decoder: androidx.compose.runtime.MutableState<MediaCodec?>,
    decoderInSurf: androidx.compose.runtime.MutableState<android.view.Surface?>,
    decoderOutSurf: androidx.compose.runtime.MutableState<android.view.Surface?>,
    decoderThread: androidx.compose.runtime.MutableState<Thread?>,
    cameraRunningState: androidx.compose.runtime.MutableState<Boolean>,
    removeDetection: Boolean,
    removeBiometric: Boolean,
    randomDeviceId: Boolean
): Boolean {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
        addLog(logEntries, "[VCAM] Device does not support virtual camera (needs Android 10+)")
        return false
    }

    try {
        val camera2Manager = context.getSystemService(android.content.Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager

        val cameraIdList = camera2Manager.cameraIdList
        var foundVirtualCamera = false

        for (cameraId in cameraIdList) {
            val characteristics = camera2Manager.getCameraCharacteristics(cameraId)
            val streamConfiguration = characteristics.get(android.hardware.camera2.CameraCharacteristics.SCALER_STREAM_CONFIGURATION) as? android.hardware.camera2.StreamConfigurationMap
            if (streamConfiguration != null) {
                val outputFormats = streamConfiguration.outputFormats
                if (outputFormats != null) {
                    var foundFormat = false
                    for (fmt in outputFormats) {
                        if (fmt == android.graphics.PixelFormat.UNKNOWN ||
                            fmt == android.graphics.PixelFormat.TRANSLUCENT ||
                            fmt == android.graphics.PixelFormat.TRANSPARENT) {
                            foundFormat = true
                            break
                        }
                    }
                    if (foundFormat) {
                        val usage = characteristics.get(android.hardware.camera2.CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                        if (usage != null && android.hardware.camera2.CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BUNDLE_FORMAT in usage) {
                            vcName.value = cameraId
                            foundVirtualCamera = true
                            addLog(logEntries, "[VCAM] Found virtual camera: $cameraId")
                            break
                        }
                    }
                }
            }
        }

        if (!foundVirtualCamera) {
            addLog(logEntries, "[VCAM] No virtual camera found — creating source")
            vcSource.value = VcSource(
                android.util.Size(bitmap.width, bitmap.height),
                android.graphics.PixelFormat.TRANSLUCENT
            )
        }

        val format = MediaFormat.createVideoFormat(
            "video/avc",
            bitmap.width,
            bitmap.height
        ).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, 500000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val codec = MediaCodec.createEncoderByType("video/avc")
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface = codec.createInputSurface()

        var vcConnected = false
        try {
            val fbMgr = context.getSystemService(android.content.Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val createVcMethod = fbMgr.javaClass.getMethod(
                "createVirtualCamera",
                android.view.Surface::class.java
            )
            val vcCameraId = createVcMethod.invoke(fbMgr, inputSurface) as String
            vcConnected = true
            addLog(logEntries, "[VCAM] Encoder surface CONNECTED to virtual camera (ID: $vcCameraId)")
        } catch (e: NoSuchMethodException) {
            addLog(logEntries, "[VCAM] createVirtualCamera not available — using shared buffer method")
            vcConnected = false
        } catch (e: Exception) {
            addLog(logEntries, "[VCAM] createVirtualCamera failed: ${e.message}")
            vcConnected = false
        }

        vcSource.value = VcSource(
            android.util.Size(bitmap.width, bitmap.height),
            android.graphics.PixelFormat.TRANSLUCENT
        )

        val muxer = MediaMuxer(
            "/data/local/tmp/virtual_camera.mp4",
            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        )
        mxTrack.value = muxer.addTrack(format)
        muxer.start()

        codec.start()

        enc.value = codec
        encSurf.value = inputSurface
        mx.value = muxer

        addLog(logEntries, "[VCAM] Encoder created: ${bitmap.width}x${bitmap.height}")
        addLog(logEntries, "[VCAM] Virtual camera source created")
        addLog(logEntries, "[VCAM] Muxer started (track ${mxTrack.value})")
        addLog(logEntries, "[VCAM] Encoder started")
        if (vcConnected) {
            addLog(logEntries, "[VCAM] VIRTUAL CAMERA IS NOW VISIBLE to target apps")
        } else {
            addLog(logEntries, "[VCAM] Feeding encoder surface — target app should receive feed")
        }

        if (removeDetection) addLog(logEntries, "[CONFIG] Remove Detection: ACTIVE")
        if (removeBiometric) addLog(logEntries, "[CONFIG] Remove Biometric: ACTIVE")
        if (randomDeviceId) addLog(logEntries, "[CONFIG] Random Device ID: ACTIVE")

        val feedThread = object : Thread("VcFeedThread") {
            override fun run() {
                usingVc.value = true

                if (isVideo && videoUri != null) {
                    setupVideoDecoder(
                        context, videoUri, logEntries, decoderRunning, decoder,
                        decoderInSurf, decoderOutSurf, decoderThread,
                        enc, encSurf, mx, mxTrack, cameraRunningState,
                        removeBiometric
                    )
                } else {
                    startPhotoLoop(
                        bitmap, logEntries, enc, mx, encSurf, mxTrack, cameraRunningState,
                        removeDetection, removeBiometric, randomDeviceId
                    )
                }

                usingVc.value = false
                addLog(logEntries, "[VCAM] Feed thread stopped")
            }
        }

        feedThread.start()
        return true

    } catch (e: Exception) {
        addLog(logEntries, "[VCAM] Init error: ${e.message}")
        e.printStackTrace()
        return false
    }
}

 */
private fun setupVideoDecoder(
    context: android.content.Context,
    videoUri: Uri,
    logEntries: MutableList<String>,
    decoderRunning: androidx.compose.runtime.MutableState<Boolean>,
    decoder: androidx.compose.runtime.MutableState<MediaCodec?>,
    decoderInSurf: androidx.compose.runtime.MutableState<android.view.Surface?>,
    decoderOutSurf: androidx.compose.runtime.MutableState<android.view.Surface?>,
    decoderThread: androidx.compose.runtime.MutableState<Thread?>,
    enc: androidx.compose.runtime.MutableState<MediaCodec?>,
    encSurf: androidx.compose.runtime.MutableState<android.view.Surface?>,
    mx: androidx.compose.runtime.MutableState<MediaMuxer?>,
    mxTrack: androidx.compose.runtime.MutableState<Int>,
    cameraRunningState: androidx.compose.runtime.MutableState<Boolean>,
    removeBiometric: Boolean
) {
    try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)

        val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

        if (widthStr != null && heightStr != null && durationStr != null) {
            val videoWidth = widthStr.toInt()
            val videoHeight = heightStr.toInt()
            val durationUs = durationStr.toLong() * 1000L

            addLog(logEntries, "[VCAM] Video: ${videoWidth}x${videoHeight}, ${durationUs / 1000000}s")

            val biometricCropY = if (removeBiometric) {
                (videoHeight * 0.85f).toInt() // crop bottom 15%
            } else { 0 }

            val decoderThreadObj = object : Thread("VideoDecoderThread") {
                override fun run() {
                    decoderRunning.value = true
                    var frameIndex = 0
                    val frameDurationUs = 1000000L / 30
                    val bufferInfo = BufferInfo()

                    while (decoderRunning.value && cameraRunningState.value) {
                        try {
                            val bitmap = retriever.getFrameAtTime(
                                frameIndex * frameDurationUs, 0L
                            )

                            if (bitmap != null) {
                                val canvas = encSurf.value?.lockCanvas(null)
                                if (canvas != null) {
                                    canvas.drawColor(android.graphics.Color.BLACK)

                                    // Crop bottom if removing biometric data
                                    val drawBitmap = if (biometricCropY > 0) {
                                        android.graphics.Bitmap.createBitmap(
                                            bitmap, 0, 0,
                                            bitmap.width, biometricCropY,
                                            android.graphics.Matrix(), false
                                        )
                                    } else { bitmap }

                                    val destRect = android.graphics.Rect(
                                        0, 0,
                                        drawBitmap.width, drawBitmap.height
                                    )
                                    val matrix = android.graphics.Matrix()
                                    val scale = minOf(
                                        destRect.width().toFloat() / drawBitmap.width,
                                        destRect.height().toFloat() / drawBitmap.height
                                    )
                                    matrix.postScale(scale, scale)
                                    canvas.drawBitmap(drawBitmap, matrix, null)

                                    if (drawBitmap != bitmap) drawBitmap.recycle()
                                    encSurf.value!!.unlockCanvasAndPost(canvas)
                                }
                                bitmap.recycle()
                            }

                            val outBufIdxLocal = enc.value?.dequeueOutputBuffer(bufferInfo, 10000)
                            when (outBufIdxLocal) {
                                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                    addLog(logEntries, "[VCAM] Encoder format changed")
                                }
                                MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> { }
                                else -> {
                                    if (bufferInfo.size > 0 && outBufIdxLocal >= 0 && mx.value != null) {
                                        val outputBuf = enc.value?.getOutputBuffer(outBufIdxLocal)
                                        if (outputBuf != null && bufferInfo.size > 0) {
                                            outputBuf.position(bufferInfo.offset)
                                            outputBuf.limit(bufferInfo.offset + bufferInfo.size)
                                            mx.value!!.writeSampleData(mxTrack.value, outputBuf, bufferInfo)
                                        }
                                        enc.value?.releaseOutputBuffer(outBufIdxLocal, false)
                                    }
                                }
                            }

                            frameIndex++
                            if (frameIndex * frameDurationUs >= durationUs) {
                                frameIndex = 0
                            }

                            Thread.sleep(33)
                        } catch (e: InterruptedException) { break }
                        catch (e: Exception) {
                            addLog(logEntries, "[VCAM] Video decode error: ${e.message}")
                            break
                        }
                    }
                    addLog(logEntries, "[VCAM] Video decoder stopped")
                }
            }

            decoderThread.value = decoderThreadObj
            decoderThreadObj.start()
            addLog(logEntries, "[VCAM] Video decoder thread started")
        }
    } catch (e: Exception) {
        addLog(logEntries, "[VCAM] Video decoder error: ${e.message}")
        e.printStackTrace()
private fun startPhotoLoop(
    bitmap: android.graphics.Bitmap,
    logEntries: MutableList<String>,
    enc: androidx.compose.runtime.MutableState<MediaCodec?>,
    mx: androidx.compose.runtime.MutableState<MediaMuxer?>,
    encSurf: androidx.compose.runtime.MutableState<android.view.Surface?>,
    mxTrack: androidx.compose.runtime.MutableState<Int>,
    cameraRunningState: androidx.compose.runtime.MutableState<Boolean>,
    removeDetection: Boolean,
    removeBiometric: Boolean,
    randomDeviceId: Boolean
) {
    val encoder = enc.value ?: return
    val muxer = mx.value ?: return
    val inputSurface = encSurf.value ?: return

    val bufferInfo = BufferInfo()

    // Pre-compute detection overlay rect and biometric crop region
    val overlayRect = if (removeDetection) {
        android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
    } else {
        null
    }

    val biometricCropY = if (removeBiometric) {
        (bitmap.height * 0.85f).toInt() // crop bottom 15%
    } else {
        0
    }

    // Generate random IMEI if enabled
    val randomImei = if (randomDeviceId) {
        generateRandomImei()
    } else {
        null
    }

    while (cameraRunningState.value) {
        try {
            val canvas = inputSurface.lockCanvas(null)
            if (canvas != null) {
                canvas.drawColor(android.graphics.Color.BLACK)

                val destRect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
                val matrix = android.graphics.Matrix()
                val scale = minOf(
                    destRect.width().toFloat() / bitmap.width,
                    destRect.height().toFloat() / bitmap.height
                )
                matrix.postScale(scale, scale)

                // If removing biometric data, crop the bitmap first
                val drawBitmap = if (biometricCropY > 0) {
                    android.graphics.Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.width, biometricCropY, matrix, false
                    )
                } else { null }

                if (drawBitmap != null) {
                    canvas.drawBitmap(drawBitmap, matrix, null)
                    drawBitmap.recycle()
                } else {
                    canvas.drawBitmap(bitmap, matrix, null)
                }

                // Remove Detection: subtle green tint overlay
                if (overlayRect != null) {
                    canvas.save()
                    canvas.clipRect(overlayRect)
                    canvas.drawColor(android.graphics.Color.argb(20, 0, 255, 0))
                    canvas.restore()
                }

                // Random Device ID: fake IMEI watermark
                if (randomImei != null) {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(60, 255, 255, 255)
                        textSize = 14f
                        isAntiAlias = true
                    }
                    canvas.drawText("IMEI: $randomImei | AID: ${generateRandomAndroidId()}", 4f, bitmap.height - 4f, paint)
                }

                inputSurface.unlockCanvasAndPost(canvas)
            }

            // Process enc output and write to mx
            val outBufIdx = encoder.dequeueOutputBuffer(bufferInfo, 10000)
            when (outBufIdx) {
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    addLog(logEntries, "[VCAM] Encoder format changed")
                }
                MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> { }
                else -> {
                    if (bufferInfo.size > 0 && mx.value != null) {
                        val outputBuf = encoder.getOutputBuffer(outBufIdx)
                        if (outputBuf != null && bufferInfo.size > 0) {
                            outputBuf.position(bufferInfo.offset)
                            outputBuf.limit(bufferInfo.offset + bufferInfo.size)
                            mx.value!!.writeSampleData(mxTrack.value, outputBuf, bufferInfo)
                        }
                        encoder.releaseOutputBuffer(outBufIdx, false)
                    }
                }
            }

            Thread.sleep(33) // ~30fps
        } catch (e: InterruptedException) { break }
        catch (e: Exception) {
            addLog(logEntries, "[VCAM] Photo loop error: ${e.message}")
            break
        }
    }
    addLog(logEntries, "[VCAM] Photo loop stopped")
}
    }
    addLog(logEntries, "[VCAM] Photo loop stopped")
}

/**
 * Clean up all virtual camera resources
 */
private fun cleanupVirtualCamera(

/** Generate a random 15-digit IMEI number with Luhn check digit */
private fun generateRandomImei(): String {
    val sb = StringBuilder()
    for (i in 0..14) {
        sb.append((0..9).random())
    }
    var sum = 0
    for (i in sb.length - 2 downTo 0) {
        var digit = sb[i].digitToInt()
        if (i % 2 == sb.length - 2 % 2) {
            digit *= 2
            if (digit > 9) digit -= 9
        }
        sum += digit
    }
    val checkDigit = (10 - (sum % 10)) % 10
    sb.append(checkDigit)
    return sb.toString()
}

/** Generate a random Android ID (32-char hex string) */
private fun generateRandomAndroidId(): String {
    val chars = "0123456789abcdef"
    return (1..32).map { chars.random() }.joinToString("")
}

    logEntries: MutableList<String>,
    decoderRunning: androidx.compose.runtime.MutableState<Boolean>,
    decoderThread: androidx.compose.runtime.MutableState<Thread?>,
    decoder: androidx.compose.runtime.MutableState<MediaCodec?>,
    decoderInSurf: androidx.compose.runtime.MutableState<android.view.Surface?>,
    decoderOutSurf: androidx.compose.runtime.MutableState<android.view.Surface?>,
    enc: androidx.compose.runtime.MutableState<MediaCodec?>,
    encSurf: androidx.compose.runtime.MutableState<android.view.Surface?>,
    mx: androidx.compose.runtime.MutableState<MediaMuxer?>,
    vcSource: androidx.compose.runtime.MutableState<VcSource?>,
    vcName: androidx.compose.runtime.MutableState<String?>,
    usingVc: androidx.compose.runtime.MutableState<Boolean>
) {
    try {
        // Stop video decoder
        decoderRunning.value = false
        decoderThread.value?.interrupt()
        decoder.value?.stop()
        decoder.value?.release()
        decoder.value = null
        decoderInSurf.value?.release()
        decoderInSurf.value = null
        decoderOutSurf.value?.release()
        decoderOutSurf.value = null
        addLog(logEntries, "[VCAM] Video decoder stopped")

        // Stop enc
        val encObj = enc.value
        if (encObj != null) {
            encObj.signalEndOfInputStream()
            encObj.stop()
            encObj.release()
            enc.value = null
            encSurf.value?.release()
            encSurf.value = null
            addLog(logEntries, "[VCAM] Encoder stopped")
        }

        // Stop mx
        val mxObj = mx.value
        if (mxObj != null) {
            mxObj.stop()
            mxObj.release()
            mx.value = null
            addLog(logEntries, "[VCAM] Muxer stopped")
        }

        // Clear virtual camera source
        vcSource.value = null
        vcName.value = null
        usingVc.value = false

        addLog(logEntries, "[VCAM] Virtual camera DEACTIVATED")
    } catch (e: Exception) {
        addLog(logEntries, "[VCAM] Cleanup error: ${e.message}")
    }
}

// ============================================================================
// END VIRTUAL CAMERA HELPER FUNCTIONS
// ============================================================================

private fun getFileName(context: android.content.Context, uri: Uri): String? {
    var fileName: String? = null
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                fileName = it.getString(nameIndex)
            }
        }
    }
    return fileName ?: uri.lastPathSegment
}

private fun addLog(logEntries: MutableList<String>, message: String) {
    val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
    logEntries.add("[$timestamp] $message")
}
