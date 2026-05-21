package com.example

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    object LevelSelect : Screen()
    data class GamePlay(
        val levelNumber: Int, // 1..50 for predefined levels, or negative for custom image play
        val gridSize: Int,
        val isCustomImage: Boolean,
        val customImageUri: String?
    ) : Screen()
}

class MainActivity : ComponentActivity() {
    private lateinit var userPrefs: UserPreferences
    private lateinit var soundSynth: SoundSynth
    private lateinit var ttsManager: TtsManager
    private lateinit var levelStatsRepository: LevelStatsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initializing core systems
        userPrefs = UserPreferences(this)
        soundSynth = SoundSynth()
        ttsManager = TtsManager(this)
        
        val db = AppDatabase.getDatabase(this)
        levelStatsRepository = LevelStatsRepository(db.levelStatsDao())

        // Welcome Text to Speech announcement
        ttsManager.speak("Welcome to Azan Papu Puzzles!")

        enableEdgeToEdge()

        setContent {
            var darkMode by remember { mutableStateOf(userPrefs.isDarkMode) }
            MyApplicationTheme(darkTheme = darkMode) {
                MainAppContainer(
                    userPrefs = userPrefs,
                    soundSynth = soundSynth,
                    ttsManager = ttsManager,
                    levelStatsRepository = levelStatsRepository,
                    isDarkModeActive = darkMode,
                    onDarkModeChanged = {
                        darkMode = it
                        userPrefs.isDarkMode = it
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        ttsManager.shutdown()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (::soundSynth.isInitialized) {
            soundSynth.setAppInForeground(true)
        }
    }

    override fun onPause() {
        if (::soundSynth.isInitialized) {
            soundSynth.setAppInForeground(false)
        }
        super.onPause()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainAppContainer(
    userPrefs: UserPreferences,
    soundSynth: SoundSynth,
    ttsManager: TtsManager,
    levelStatsRepository: LevelStatsRepository,
    isDarkModeActive: Boolean,
    onDarkModeChanged: (Boolean) -> Unit
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    // Read stored settings values in Compose
    var soundOn by remember { mutableStateOf(userPrefs.isSoundOn) }
    var musicOn by remember { mutableStateOf(userPrefs.isMusicOn) }
    var watermarkOn by remember { mutableStateOf(userPrefs.isWatermarkOn) }
    var coinsCount by remember { mutableStateOf(userPrefs.coins) }
    var showNumbers by remember { mutableStateOf(userPrefs.showNumbers) }

    // Reactive lifecycle observer to cleanly pause synthesizer output and loop execution
    var isAppInForeground by remember { mutableStateOf(true) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    isAppInForeground = true
                    soundSynth.setAppInForeground(true)
                }
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    isAppInForeground = false
                    soundSynth.setAppInForeground(false)
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Background dynamic synthesizer tune loop (Cozy Game Ambient music)
    LaunchedEffect(musicOn, isAppInForeground) {
        if (musicOn && isAppInForeground) {
            while (true) {
                // Soft C major 9th progression note cascade (subtle, professional, non-fatiguing)
                soundSynth.playTone(frequency = 130.81, durationMs = 1500, type = SoundSynth.WaveType.SINE, volume = 0.08f) // C3 Bass Pad
                delay(300)
                soundSynth.playTone(frequency = 329.63, durationMs = 800, type = SoundSynth.WaveType.SINE, volume = 0.04f) // E4 Subtle chime
                delay(1200)

                if (!isAppInForeground) break

                soundSynth.playTone(frequency = 174.61, durationMs = 1500, type = SoundSynth.WaveType.SINE, volume = 0.08f) // F3 Bass Pad
                delay(300)
                soundSynth.playTone(frequency = 440.00, durationMs = 800, type = SoundSynth.WaveType.SINE, volume = 0.04f) // A4 Subtle chime
                delay(1200)

                if (!isAppInForeground) break

                soundSynth.playTone(frequency = 196.00, durationMs = 1500, type = SoundSynth.WaveType.SINE, volume = 0.08f) // G3 Bass Pad
                delay(300)
                soundSynth.playTone(frequency = 392.00, durationMs = 800, type = SoundSynth.WaveType.SINE, volume = 0.04f) // G4 Subtle chime
                delay(3000)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (isDarkModeActive) Color(0xFF121212) else Color(0xFFF8F9FF))
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn() togetherWith
                    slideOutHorizontally(targetOffsetX = { -1000 }) + fadeOut()
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    is Screen.Home -> {
                        HomeScreenView(
                            coins = coinsCount,
                            soundOn = soundOn,
                            musicOn = musicOn,
                            watermarkOn = watermarkOn,
                            showNumbers = showNumbers,
                            isDarkMode = isDarkModeActive,
                            onPlayLevelsSelected = { currentScreen = Screen.LevelSelect },
                            onCustomImageSelected = { gridSize, uri ->
                                currentScreen = Screen.GamePlay(
                                    levelNumber = -gridSize, // negative values denote custom image matching its gridSize
                                    gridSize = gridSize,
                                    isCustomImage = true,
                                    customImageUri = uri.toString()
                                )
                            },
                            onSettingsSaved = { sound, music, watermark, num, dark ->
                                soundOn = sound
                                userPrefs.isSoundOn = sound
                                musicOn = music
                                userPrefs.isMusicOn = music
                                watermarkOn = watermark
                                userPrefs.isWatermarkOn = watermark
                                showNumbers = num
                                userPrefs.showNumbers = num
                                onDarkModeChanged(dark)
                            }
                        )
                    }
                    is Screen.LevelSelect -> {
                        LevelSelectScreenView(
                            levelStatsRepository = levelStatsRepository,
                            isDarkMode = isDarkModeActive,
                            onBackToHome = { currentScreen = Screen.Home },
                            onLevelSelected = { level, size ->
                                currentScreen = Screen.GamePlay(
                                    levelNumber = level,
                                    gridSize = size,
                                    isCustomImage = false,
                                    customImageUri = null
                                )
                            }
                        )
                    }
                    is Screen.GamePlay -> {
                        GamePlayScreenView(
                            levelNumber = targetScreen.levelNumber,
                            gridSize = targetScreen.gridSize,
                            isCustomImage = targetScreen.isCustomImage,
                            customImageUri = targetScreen.customImageUri,
                            soundEnabled = soundOn,
                            watermarkEnabled = watermarkOn,
                            showNumbersEnabled = showNumbers,
                            isDarkMode = isDarkModeActive,
                            levelStatsRepository = levelStatsRepository,
                            soundSynth = soundSynth,
                            ttsManager = ttsManager,
                            onAddCoins = { amount ->
                                userPrefs.coins += amount
                                coinsCount = userPrefs.coins
                            },
                            onExitGame = {
                                currentScreen = if (targetScreen.levelNumber < 0) Screen.Home else Screen.LevelSelect
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// HOME SCREEN VIEW
// ==========================================
@Composable
fun HomeScreenView(
    coins: Int,
    soundOn: Boolean,
    musicOn: Boolean,
    watermarkOn: Boolean,
    showNumbers: Boolean,
    isDarkMode: Boolean,
    onPlayLevelsSelected: () -> Unit,
    onCustomImageSelected: (Int, Uri) -> Unit,
    onSettingsSaved: (Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    val context = LocalContext.current
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isSupportOpen by remember { mutableStateOf(false) }
    var customImagePickerUri by remember { mutableStateOf<Uri?>(null) }
    var isGridSizeSelectionOpen by remember { mutableStateOf(false) }

    // Theme calculation
    val isDark = isDarkMode
    val appBackgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFF8F9FF)
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val cardBackgroundColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val borderStrokeColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val textSecondaryColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            customImagePickerUri = uri
            isGridSizeSelectionOpen = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Status Bar lookalike + Coin display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Simulated Time Indicator
            Text(
                text = "9:41",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.6f)
            )

            // Dynamic Gold Coin Pill Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7)) // amber-900 / amber-100
                    .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .testTag("coins_indicator")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$coins",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Color(0xFFF59E0B) // text-amber-500
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(Color(0xFFF59E0B), CircleShape), // bg-amber-500
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Hero Branding Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            // Puzzle Grid Artwork (Individually rotated/overlapping cubes representing AzanPapu blocks)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                // Stack of color cards matching Tailwind rotates
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .graphicsLayer(rotationZ = 6f)
                ) {
                    // Grid mapping matching Tailwind representation
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Indigo card
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .shadow(4.dp, RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF4F46E5))
                            )
                            // Sky card
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .graphicsLayer(translationY = 8f)
                                    .shadow(4.dp, RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0EA5E9))
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Rose card
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .graphicsLayer(translationX = -8f)
                                    .shadow(4.dp, RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFB7185))
                            )
                            // Deep indigo card
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .shadow(4.dp, RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E1B4B))
                            )
                        }
                    }
                }
                
                // Active status little circle floating
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.BottomEnd)
                        .graphicsLayer(rotationZ = -12f, translationX = -4f, translationY = -4f)
                        .shadow(6.dp, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(cardBackgroundColor)
                        .border(0.5.dp, borderStrokeColor, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4ADE80)) // green-400
                    )
                }
            }

            // Beautiful display typography
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = textColor, fontWeight = FontWeight.Black)) {
                        append("AzanPapu\n")
                    }
                    withStyle(style = SpanStyle(color = Color(0xFF4F46E5), fontWeight = FontWeight.Black)) {
                        append("Puzzles")
                    }
                },
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                lineHeight = 44.sp,
                modifier = Modifier.testTag("app_title")
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "BRAIN EXERCISE & FUN",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                color = textSecondaryColor
            )
        }

        // Action Options Area
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Main Button: Play Levels
            Button(
                onClick = onPlayLevelsSelected,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(8.dp, RoundedCornerShape(24.dp))
                    .testTag("play_levels_button")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Play Levels",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Arrow right icon",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Secondary Button: Gallery Upload
            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = cardBackgroundColor),
                border = BorderStroke(1.dp, borderStrokeColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(1.dp, RoundedCornerShape(24.dp))
                    .testTag("upload_pic_button")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Light sky image block icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(if (isDark) Color(0xFF0C4A6E) else Color(0xFFF0F9FF), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Gallery block icon",
                                tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Gallery Upload",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(if (isDark) Color(0xFF0369A1) else Color(0xFFE0F2FE), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "NEW",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0369A1),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Grid column of action keys (Settings + Support)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Settings action button
                OutlinedButton(
                    onClick = { isSettingsOpen = true },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = cardBackgroundColor),
                    border = BorderStroke(1.dp, borderStrokeColor),
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .testTag("settings_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Settings gear icon",
                            tint = textSecondaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Settings",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }

                // Support contact button
                OutlinedButton(
                    onClick = { isSupportOpen = true },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = cardBackgroundColor),
                    border = BorderStroke(1.dp, borderStrokeColor),
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF4C0519) else Color(0xFFFFE4E6)), // rose-950 / rose-100
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF43F5E)) // rose-500
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Support",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }
        }

        // Footer Brand Info Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                text = buildAnnotatedString {
                    append("A game by ")
                    withStyle(style = SpanStyle(color = if (isDark) Color(0xFFC7D2FE) else Color(0xFF1E1B4B), fontWeight = FontWeight.Bold)) {
                        append("MCK AI STUDIO")
                    }
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = textSecondaryColor,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(5.dp)
                    .background(borderStrokeColor, RoundedCornerShape(100.dp))
            )
        }
    }

    // Grid selection modal for Custom Image Pick
    if (isGridSizeSelectionOpen && customImagePickerUri != null) {
        Dialog(onDismissRequest = { isGridSizeSelectionOpen = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, borderStrokeColor),
                modifier = Modifier.padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Select Grid Complexity",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = textColor
                    )
                    Text(
                        text = "Customize the difficulty of your custom image sliding blocks:",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = textSecondaryColor
                    )

                    Button(
                        onClick = {
                            isGridSizeSelectionOpen = false
                            onCustomImageSelected(3, customImagePickerUri!!)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("3x3 Grid (Easy)", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            isGridSizeSelectionOpen = false
                            onCustomImageSelected(4, customImagePickerUri!!)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("4x4 Grid (Medium)", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            isGridSizeSelectionOpen = false
                            onCustomImageSelected(5, customImagePickerUri!!)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFB7185)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("5x5 Grid (Hard)", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    TextButton(onClick = { isGridSizeSelectionOpen = false }) {
                        Text("Cancel", color = textSecondaryColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // Settings Configuration Dialog
    if (isSettingsOpen) {
        var tempSound by remember { mutableStateOf(soundOn) }
        var tempMusic by remember { mutableStateOf(musicOn) }
        var tempWatermark by remember { mutableStateOf(watermarkOn) }
        var tempNumbers by remember { mutableStateOf(showNumbers) }
        var tempDark by remember { mutableStateOf(isDarkMode) }

        Dialog(onDismissRequest = { isSettingsOpen = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                border = BorderStroke(1.dp, borderStrokeColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .shadow(12.dp, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Game Settings ⚙️",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Sound Effects switch tile
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sound Effects", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Switch(
                            checked = tempSound,
                            onCheckedChange = { tempSound = it },
                            modifier = Modifier.testTag("sound_switch")
                        )
                    }

                    // Background Music switch tile
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Background Synths", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Switch(
                            checked = tempMusic,
                            onCheckedChange = { tempMusic = it },
                            modifier = Modifier.testTag("music_switch")
                        )
                    }

                    // Show numbers helper switch tile
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Block Numbers", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Switch(
                            checked = tempNumbers,
                            onCheckedChange = { tempNumbers = it },
                            modifier = Modifier.testTag("numbers_switch")
                        )
                    }

                    // Dark Mode switch tile
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dark Visual Theme", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Switch(
                            checked = tempDark,
                            onCheckedChange = { tempDark = it },
                            modifier = Modifier.testTag("dark_switch")
                        )
                    }

                    // Watermark trade branding toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("MCK Studio Watermark", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Switch(
                            checked = tempWatermark,
                            onCheckedChange = { tempWatermark = it },
                            modifier = Modifier.testTag("watermark_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Closing save buttons
                    Button(
                        onClick = {
                            isSettingsOpen = false
                            onSettingsSaved(tempSound, tempMusic, tempWatermark, tempNumbers, tempDark)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save & Close", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // Support Contact Dialog
    if (isSupportOpen) {
        Dialog(onDismissRequest = { isSupportOpen = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                border = BorderStroke(1.dp, borderStrokeColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(if (isDark) Color(0xFF4C0519) else Color(0xFFFFE4E6), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Email,
                            contentDescription = "Support Agent Icon",
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Text(
                        text = "Customer Support",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = textColor
                    )
                    
                    Text(
                        text = "Need help or have questions about AzanPapu Puzzles? Contact our team directly at:",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = textSecondaryColor
                    )

                    Text(
                        text = "support@mckstudio.agency",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF4F46E5)
                    )

                    Button(
                        onClick = { isSupportOpen = false },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color.White else Color(0xFF0F172A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dismiss", fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF0F172A) else Color.White)
                    }
                }
            }
        }
    }
}

// ==========================================
// LEVEL SELECTION SCREEN VIEW
// ==========================================
@Composable
fun LevelSelectScreenView(
    levelStatsRepository: LevelStatsRepository,
    isDarkMode: Boolean,
    onBackToHome: () -> Unit,
    onLevelSelected: (Int, Int) -> Unit
) {
    val statsList by levelStatsRepository.allStats.collectAsStateWithLifecycle(initialValue = emptyList())

    val isDark = isDarkMode
    val appBackgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFF8F9FF)
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val cardBackgroundColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val borderStrokeColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val textSecondaryColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToHome,
                modifier = Modifier
                    .background(cardBackgroundColor, CircleShape)
                    .border(1.dp, borderStrokeColor, CircleShape)
                    .testTag("back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Select Level",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = textColor
            )
        }

        // Subtitle / Grid Info
        Text(
            text = "Puzzle sizes increase as you advance:",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = textSecondaryColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Color-coded complexity legend bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0EA5E9).copy(alpha = 0.15f))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Easy 3x3 (1-15)", color = Color(0xFF0EA5E9), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF4F46E5).copy(alpha = 0.15f))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Medium 4x4 (16-35)", color = Color(0xFF4F46E5), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFB7185).copy(alpha = 0.15f))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Hard 5x5 (36-50)", color = Color(0xFFFB7185), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        val levelChunks = (1..50).toList().chunked(5)
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(levelChunks) { rowLevels ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (levelNum in rowLevels) {
                        val (gridSize, levelColor) = when {
                            levelNum <= 15 -> Pair(3, Color(0xFF0EA5E9)) // Sky Blue
                            levelNum <= 35 -> Pair(4, Color(0xFF4F46E5)) // Indigo
                            else -> Pair(5, Color(0xFFFB7185)) // Rose
                        }

                        val stats = statsList.find { it.levelNumber == levelNum }
                        val isCompleted = stats != null && stats.timesCompleted > 0

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .shadow(2.dp, RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp))
                                .background(levelColor)
                                .clickable { onLevelSelected(levelNum, gridSize) }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "$levelNum",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                if (isCompleted) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "🏆",
                                        fontSize = 11.sp
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


// ==========================================
// GAME PLAY SCREEN VIEW
// ==========================================
@Composable
fun GamePlayScreenView(
    levelNumber: Int,
    gridSize: Int,
    isCustomImage: Boolean,
    customImageUri: String?,
    soundEnabled: Boolean,
    watermarkEnabled: Boolean,
    showNumbersEnabled: Boolean,
    isDarkMode: Boolean,
    levelStatsRepository: LevelStatsRepository,
    soundSynth: SoundSynth,
    ttsManager: TtsManager,
    onAddCoins: (Int) -> Unit,
    onExitGame: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val engine = remember { PuzzleEngine(gridSize) }

    // State holders
    var puzzleState by remember { mutableStateOf(engine.createInitialState()) }
    var seconds by remember { mutableStateOf(0) }
    var showPeek by remember { mutableStateOf(false) }
    var isVictoryDialogOpen by remember { mutableStateOf(false) }

    // Theme calculations
    val isDark = isDarkMode
    val appBackgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFF8F9FF)
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val cardBackgroundColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val borderStrokeColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val textSecondaryColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    // Load picked image bitmap or procedural planet painting
    val puzzleBitmap = remember {
        if (isCustomImage && customImageUri != null) {
            try {
                loadBitmapFromUri(context, Uri.parse(customImageUri))
            } catch (e: Exception) {
                createProceduralArtwork()
            }
        } else {
            createProceduralArtwork()
        }
    }

    // Active Timer coroutine
    LaunchedEffect(puzzleState.isSolved) {
        if (!puzzleState.isSolved) {
            while (true) {
                delay(1000)
                seconds++
            }
        }
    }

    // Solved Game check - Triggers celebration
    LaunchedEffect(puzzleState.isSolved) {
        if (puzzleState.isSolved) {
            if (soundEnabled) {
                soundSynth.playWinFanfare()
            }
            ttsManager.speak("You won the puzzle in $seconds seconds with ${puzzleState.movesCount} moves! Great job!")
            
            // Record to Room Database
            coroutineScope.launch {
                levelStatsRepository.recordCompletion(levelNumber, gridSize, seconds, puzzleState.movesCount)
            }
            // Reward 10 preference coins
            onAddCoins(10)
            isVictoryDialogOpen = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header Navigation Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onExitGame,
                modifier = Modifier
                    .background(cardBackgroundColor, CircleShape)
                    .border(1.dp, borderStrokeColor, CircleShape)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Exit game", tint = textColor)
            }

            Box(
                modifier = Modifier
                    .background(if (isDark) Color(0xFF2E2E2E) else Color(0xFFEEF2F6), RoundedCornerShape(100.dp))
                    .border(1.dp, borderStrokeColor, RoundedCornerShape(100.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (levelNumber < 0) "Custom: ${gridSize}x${gridSize}" else "Level $levelNumber: ${gridSize}x${gridSize}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            // Peek Guide Button
            TextButton(
                onClick = { showPeek = !showPeek },
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.textButtonColors(containerColor = cardBackgroundColor),
                border = BorderStroke(1.dp, borderStrokeColor),
                modifier = Modifier.height(38.dp)
            ) {
                Icon(Icons.Filled.Info, contentDescription = "Peek", tint = Color(0xFF4F46E5), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (showPeek) "Hide Reference" else "Show Reference",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF4F46E5)
                )
            }
        }

        // Live stats header panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBackgroundColor, RoundedCornerShape(24.dp))
                .border(1.dp, borderStrokeColor, RoundedCornerShape(24.dp))
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DECISIVE TIME", fontSize = 10.sp, fontWeight = FontWeight.Black, color = textSecondaryColor, letterSpacing = 0.5.sp)
                Text(
                    text = "${seconds}s",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color(0xFFF87171) else Color(0xFFEF4444)
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(borderStrokeColor)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("MOVES TAKEN", fontSize = 10.sp, fontWeight = FontWeight.Black, color = textSecondaryColor, letterSpacing = 0.5.sp)
                Text(
                    text = "${puzzleState.movesCount}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF4F46E5)
                )
            }
        }

        // Interactive Puzzle Arena
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .aspectRatio(1f)
                .shadow(2.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(cardBackgroundColor)
                .border(2.dp, borderStrokeColor, RoundedCornerShape(24.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (showPeek) {
                // Peek guide - Visual solves helper
                if (puzzleBitmap != null) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawImage(
                            image = puzzleBitmap.asImageBitmap(),
                            dstSize = IntSize(size.width.toInt(), size.height.toInt())
                        )
                    }
                }
            } else {
                // Sliding block grid layout
                Column(modifier = Modifier.fillMaxSize()) {
                    for (row in 0 until gridSize) {
                        Row(modifier = Modifier.weight(1f)) {
                            for (col in 0 until gridSize) {
                                val cellIndex = row * gridSize + col
                                val tileValue = puzzleState.tiles[cellIndex]

                                PuzzleTile(
                                    gridSize = gridSize,
                                    tileValue = tileValue,
                                    showNumbers = showNumbersEnabled,
                                    isDarkMode = isDark,
                                    onClick = {
                                        puzzleState = engine.makeMove(puzzleState, cellIndex) {
                                            if (soundEnabled) soundSynth.playSlideSound()
                                        }
                                    },
                                    bitmap = puzzleBitmap,
                                    isSolved = puzzleState.isSolved,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Trademark floating Watermark (if enabled by configs)
            if (watermarkEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Text(
                        text = "MCK AI STUDIO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor.copy(alpha = 0.28f),
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .background(cardBackgroundColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Action Toggles inside Gameplay Page
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Restart button
            OutlinedButton(
                onClick = {
                    puzzleState = engine.createInitialState()
                    seconds = 0
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = cardBackgroundColor),
                border = BorderStroke(1.dp, borderStrokeColor)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Restart", tint = textColor)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Board", fontWeight = FontWeight.Bold, color = textColor)
            }

            // Cheat Button (Instant solver simulation for verification / reviews)
            Button(
                onClick = {
                    // Cheat trigger: set solved state directly
                    puzzleState = puzzleState.copy(
                        tiles = (0 until gridSize * gridSize).toList(),
                        isSolved = true
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("cheat_solve_button"),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFB7185))
            ) {
                Text("Mock Solve", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }

    // Success Celebration Victory Modal Dialog
    if (isVictoryDialogOpen) {
        Dialog(onDismissRequest = { isVictoryDialogOpen = false }) {
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                border = BorderStroke(1.dp, borderStrokeColor),
                modifier = Modifier
                    .padding(16.dp)
                    .shadow(16.dp, RoundedCornerShape(32.dp))
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🏆", fontSize = 38.sp)
                    }

                    Text(
                        text = "Puzzle Solved!",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "You successfully finished the ${gridSize}x${gridSize} blocks enigma!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = textSecondaryColor
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) Color(0xFF2E2E2E) else Color(0xFFF8FAFC), RoundedCornerShape(20.dp))
                            .border(1.dp, borderStrokeColor, RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Time", color = textSecondaryColor, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("${seconds} Seconds", fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Moves", color = textSecondaryColor, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("${puzzleState.movesCount} Moves", fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Reward Coins", color = textSecondaryColor, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("+10 🪙", fontWeight = FontWeight.ExtraBold, color = Color(0xFFF59E0B), fontSize = 14.sp)
                        }
                    }

                    if (watermarkEnabled) {
                        Text(
                            text = "MCK AI STUDIO WATERMARK APPLIED ✓",
                            fontSize = 10.sp,
                            color = textSecondaryColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Button(
                        onClick = {
                            isVictoryDialogOpen = false
                            onExitGame()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text("Awesome!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// Helper: Custom slide tile
@Composable
fun PuzzleTile(
    gridSize: Int,
    tileValue: Int,
    showNumbers: Boolean,
    isDarkMode: Boolean,
    onClick: () -> Unit,
    bitmap: Bitmap?,
    isSolved: Boolean,
    modifier: Modifier = Modifier
) {
    val isEmpty = (tileValue == gridSize * gridSize - 1) && !isSolved
    val isDark = isDarkMode
    val cardBackgroundColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val borderStrokeColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)

    Box(
        modifier = modifier
            .padding(3.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isEmpty) { onClick() }
            .then(
                if (isEmpty) {
                    Modifier.background(if (isDark) Color(0xFF121212) else Color(0xFFF1F5F9))
                } else {
                    Modifier.shadow(2.dp, RoundedCornerShape(12.dp))
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!isEmpty) {
            if (bitmap != null) {
                // Sliced Image rendering
                val srcRow = tileValue / gridSize
                val srcCol = tileValue % gridSize

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val tileWidth = bitmap.width / gridSize
                    val tileHeight = bitmap.height / gridSize
                    val srcX = srcCol * tileWidth
                    val srcY = srcRow * tileHeight

                    drawImage(
                        image = bitmap.asImageBitmap(),
                        srcOffset = IntOffset(srcX, srcY),
                        srcSize = IntSize(tileWidth, tileHeight),
                        dstOffset = IntOffset(0, 0),
                        dstSize = IntSize(size.width.toInt(), size.height.toInt())
                    )
                }

                // Transparent round target circles for pristine readability without obscuring full artworks
                if (showNumbers) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${tileValue + 1}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            } else {
                // Indigo color spectrum
                val baseColor = when (tileValue % 3) {
                    0 -> Color(0xFF4F46E5) // Sleek Indigo
                    1 -> Color(0xFF0EA5E9) // Sky Blue
                    else -> Color(0xFF6366F1) // Cohesive Indigo-600
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(baseColor, baseColor.copy(alpha = 0.85f))
                             )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (showNumbers) {
                        Text(
                            text = "${tileValue + 1}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 20.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

// Utility: Uri load helper
fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Utility: Procedural art generator drawing planet
fun createProceduralArtwork(width: Int = 800, height: Int = 800): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint()

    val lg = android.graphics.LinearGradient(
        0f, 0f, width.toFloat(), height.toFloat(),
        android.graphics.Color.parseColor("#3F51B5"), // Indigo
        android.graphics.Color.parseColor("#FF4081"), // Pink
        android.graphics.Shader.TileMode.CLAMP
    )
    paint.shader = lg
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    paint.shader = null

    // Tiny cosmic dust
    val rand = java.util.Random(101)
    for (i in 0 until 18) {
        val radius = 20f + rand.nextFloat() * 60f
        val x = rand.nextFloat() * width
        val y = rand.nextFloat() * height
        
        paint.color = android.graphics.Color.argb(
            70, 
            rand.nextInt(256), 
            rand.nextInt(256), 
            255
        )
        canvas.drawCircle(x, y, radius, paint)
    }

    // Main central smiling star/planet block
    paint.color = android.graphics.Color.parseColor("#FFC107")
    canvas.drawCircle(width / 2f, height / 2f, 170f, paint)

    // Saturn ring outline
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = 26f
    paint.color = android.graphics.Color.parseColor("#E91E63")
    val oval = android.graphics.RectF(width / 2f - 260f, height / 2f - 55f, width / 2f + 260f, height / 2f + 55f)
    canvas.drawOval(oval, paint)

    // Face features
    paint.style = android.graphics.Paint.Style.FILL
    paint.color = android.graphics.Color.BLACK
    canvas.drawCircle(width / 2f - 50f, height / 2f - 15f, 16f, paint)
    canvas.drawCircle(width / 2f + 50f, height / 2f - 15f, 16f, paint)

    // Shimmer
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(width / 2f - 54f, height / 2f - 20f, 6f, paint)
    canvas.drawCircle(width / 2f + 46f, height / 2f - 20f, 6f, paint)

    // Cheeks
    paint.color = android.graphics.Color.parseColor("#FF5722")
    canvas.drawCircle(width / 2f - 90f, height / 2f + 30f, 22f, paint)
    canvas.drawCircle(width / 2f + 90f, height / 2f + 30f, 22f, paint)

    // Smiling line
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = 14f
    paint.color = android.graphics.Color.BLACK
    val path = android.graphics.Path()
    path.moveTo(width / 2f - 40f, height / 2f + 50f)
    path.quadTo(width / 2f, height / 2f + 100f, width / 2f + 40f, height / 2f + 50f)
    canvas.drawPath(path, paint)

    // Header label
    paint.style = android.graphics.Paint.Style.FILL
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 55f
    paint.isFakeBoldText = true
    paint.textAlign = android.graphics.Paint.Align.CENTER
    canvas.drawText("AZANPAPU PUZZLES", width / 2f, 150f, paint)

    return bitmap
}
