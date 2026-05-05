package com.example.orbitlist

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Search
import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.DialogProperties
import com.example.orbitlist.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_School)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("OrbitListPrefs", Context.MODE_PRIVATE) }
            var showLanding by remember { mutableStateOf(prefs.getBoolean("is_first_run", true)) }
            var splashPhase by remember { mutableStateOf(0) } // 0: App Splash, 1: Agency Splash, 2: Content/Landing

            LaunchedEffect(Unit) {
                delay(1500)
                splashPhase = 1
                delay(1500)
                splashPhase = 2
            }

            if (splashPhase < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (splashPhase == 0) SpaceDark else Color(0xFF1A0B2E)),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = splashPhase,
                        transitionSpec = {
                            fadeIn(tween(500)) togetherWith fadeOut(tween(500))
                        },
                        label = "splashTransition"
                    ) { phase ->
                        if (phase == 0) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "ORBIT LIST",
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 6.sp,
                                    fontSize = 32.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Orbitkan aktivitasmu",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(0.6f),
                                    letterSpacing = 1.sp
                                )
                            }
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.logo_agency),
                                contentDescription = "Agency Logo",
                                modifier = Modifier.size(200.dp)
                            )
                        }
                    }
                }
            } else if (showLanding) {
                // Landing Page / Onboarding
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A0B2E)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            "SELAMAT DATANG",
                            fontWeight = FontWeight.Black,
                            fontSize = 28.sp,
                            color = Color.White,
                            letterSpacing = 4.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Siap untuk mengorbitkan produktivitasmu ke tingkat selanjutnya?",
                            textAlign = TextAlign.Center,
                            color = Color.White.copy(0.7f),
                            lineHeight = 24.sp
                        )
                        Spacer(modifier = Modifier.height(48.dp))
                        Button(
                            onClick = {
                                prefs.edit().putBoolean("is_first_run", false).apply()
                                showLanding = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text(
                                "MULAI SEKARANG",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A0B2E),
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            } else {
                val themeStr = prefs.getString("global_theme", "Default") ?: "Default"
                
                val themeColors = when(themeStr) {
                    "Kuning" -> listOf(Color(0xFFFFD700).copy(0.15f), Color.Transparent)
                    "Hijau" -> listOf(Color(0xFF00FF41).copy(0.15f), Color.Transparent)
                    else -> listOf(ElectricIndigo.copy(0.15f), Color.Transparent)
                }
                val glowColor = when(themeStr) {
                    "Kuning" -> Color(0xFFFFD700)
                    "Hijau" -> Color(0xFF00FF41)
                    else -> ElectricIndigo
                }

                SchoolTheme {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        var hasNotificationPermission by remember {
                            mutableStateOf(
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                            )
                        }
                        val permissionLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.RequestPermission(),
                            onResult = { isGranted ->
                                hasNotificationPermission = isGranted
                            }
                        )
                        LaunchedEffect(Unit) {
                            if (!hasNotificationPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            NotificationHelper.scheduleDailyBriefing(context)
                        }
                    }
                    TodoApp(themeColors, glowColor)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TodoApp(
    themeColors: List<Color>,
    glowColor: Color,
    viewModel: TodoViewModel = viewModel()
) {
    CompositionLocalProvider(LocalHapticFeedback provides NoHapticFeedback()) {
        TodoAppContent(themeColors, glowColor, viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TodoAppContent(
    themeColors: List<Color>,
    glowColor: Color,
    viewModel: TodoViewModel
) {
    var editingItem by remember { mutableStateOf<TodoItem?>(null) }
    var detailItem by remember { mutableStateOf<TodoItem?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var showFlightLog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Harian, 1: Mingguan, 2: Agenda
    var filterCategory by remember { mutableStateOf("Semua") }
    val searchQuery by viewModel.searchQuery.collectAsState(initial = "")
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    
    val todoList by viewModel.allTasks.collectAsState(initial = emptyList())
    val streak by viewModel.streak.collectAsState()
    val categories by viewModel.categories.collectAsState()
    
    val filterCategories = listOf("Semua") + categories
    
    val filteredList = todoList.filter { 
        val matchesCategory = filterCategory == "Semua" || it.category == filterCategory
        val matchesTab = when(selectedTab) {
            0 -> it.repeatMode == "Daily"
            1 -> it.repeatMode == "Weekly"
            2 -> it.repeatMode == "None"
            else -> false
        }
        matchesCategory && matchesTab
    }

    // State untuk drag-and-drop
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffset by remember { mutableStateOf(0f) }

    val totalTasks = filteredList.size
    val completedTasks = filteredList.count { 
        if (selectedTab == 2) it.isDone 
        else {
            // For recurring tasks, consider it "done for today" if:
            // 1. isDone is true (not yet rolled over)
            // 2. OR it was completed in the last 24 hours (for Daily) or 7 days (for Weekly)
            it.isDone || (it.completedAt != null && it.completedAt!! > System.currentTimeMillis() - 24 * 60 * 60 * 1000)
        }
    }
    val progressAnim by animateFloatAsState(
        targetValue = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    // Task 3: Thematic Colors
    val (tabGlow, tabColors) = when(selectedTab) {
        0 -> Color(0xFFFFD700) to listOf(Color(0xFFFFD700), Color(0xFFFF8C00), Color(0xFFFFD700)) // Harian: Yellow/Orange
        1 -> Color(0xFFFF0000) to listOf(Color(0xFFFF0000), Color(0xFFFF69B4), Color(0xFFFF0000)) // Mingguan: Red/Pink
        2 -> Color(0xFF8A2BE2) to listOf(Color(0xFF8A2BE2), Color(0xFF0000FF), Color(0xFF00FF41), Color(0xFF8A2BE2)) // Agenda: Purple/Blue/Green
        else -> glowColor to listOf(glowColor, NeonCyan, glowColor)
    }

    val appBackground = Color(0xFF050B18)

    Box(modifier = Modifier
        .fillMaxSize()
        .background(appBackground)
        .drawBehind {
            drawCircle(
                Brush.radialGradient(themeColors),
                radius = 800f,
                center = Offset(size.width * 0.8f, size.height * 0.1f)
            )
            drawCircle(
                Brush.radialGradient(listOf(HotPink.copy(0.1f), Color.Transparent)),
                radius = 600f,
                center = Offset(size.width * 0.1f, size.height * 0.9f)
            )
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "ORBIT LIST",
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 4.sp,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Text(
                                    "Kelola aktivitasmu dalam orbit yang teratur.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(0.6f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { showHelp = true }) {
                                Icon(Icons.Default.HelpOutline, "Petunjuk Aplikasi", tint = NeonCyan)
                            }
                        },
                        actions = {
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Default.Settings, "Pengaturan", tint = NeonCyan)
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                    )
                    
                    // Search Bar
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Cari misi di galaksi...", color = Color.White.copy(0.3f)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(0.05f)),
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White.copy(0.4f)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Close, null, tint = Color.White.copy(0.4f))
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = NeonCyan,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }
            },
            floatingActionButton = {
                InteractiveFAB(glowColor, onClick = { 
                    editingItem = null
                    showSheet = true 
                })
            },
            bottomBar = {
                val context = LocalContext.current
                NavigationBar(
                    containerColor = Color(0xFF050B18).copy(0.95f),
                    contentColor = Color.White,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Today, null) },
                        label = { Text("Harian", fontSize = 9.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFFD700),
                            unselectedIconColor = Color.White.copy(0.4f),
                            selectedTextColor = Color(0xFFFFD700),
                            unselectedTextColor = Color.White.copy(0.4f),
                            indicatorColor = Color(0xFFFFD700).copy(0.1f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.DateRange, null) },
                        label = { Text("Mingguan", fontSize = 9.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFF0000),
                            unselectedIconColor = Color.White.copy(0.4f),
                            selectedTextColor = Color(0xFFFF0000),
                            unselectedTextColor = Color.White.copy(0.4f),
                            indicatorColor = Color(0xFFFF0000).copy(0.1f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.RocketLaunch, null) },
                        label = { Text("Agenda", fontSize = 9.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF8A2BE2),
                            unselectedIconColor = Color.White.copy(0.4f),
                            selectedTextColor = Color(0xFF8A2BE2),
                            unselectedTextColor = Color.White.copy(0.4f),
                            indicatorColor = Color(0xFF8A2BE2).copy(0.1f)
                        )
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { showFlightLog = true },
                        icon = { Icon(Icons.Default.QueryStats, null) },
                        label = { Text("Stats", fontSize = 9.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            unselectedIconColor = Color.White.copy(0.4f),
                            unselectedTextColor = Color.White.copy(0.4f)
                        )
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://orbitlistapp.vercel.app/"))
                            context.startActivity(intent)
                        },
                        icon = { Icon(Icons.Default.Language, null) },
                        label = { Text("Web", fontSize = 9.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            unselectedIconColor = Color.White.copy(0.4f),
                            unselectedTextColor = Color.White.copy(0.4f)
                        )
                    )
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                
                GlassCard(
                    modifier = Modifier.padding(16.dp),
                    glowColor = if (progressAnim > 0.8f) CyberGreen.copy(0.2f) else tabGlow.copy(0.2f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Section 1: Progress Circle (Fixed weight to prevent shifting)
                        Box(
                            contentAlignment = Alignment.Center, 
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(90.dp)) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawArc(
                                        color = Color.White.copy(0.05f),
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = 10f, cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        brush = Brush.sweepGradient(
                                            if (progressAnim > 0.8f && selectedTab != 2) listOf(CyberGreen, NeonCyan, CyberGreen)
                                            else tabColors
                                        ),
                                        startAngle = -90f,
                                        sweepAngle = progressAnim * 360f,
                                        useCenter = false,
                                        style = Stroke(width = 10f, cap = StrokeCap.Round)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "${(progressAnim * 100).toInt()}%", 
                                        fontWeight = FontWeight.Black, 
                                        fontSize = 20.sp, 
                                        color = Color.White
                                    )
                                    Text(
                                        if (selectedTab == 2) "PROGRES" else "PROGRES",
                                        fontSize = 9.sp, 
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp, 
                                        color = if (progressAnim > 0.8f) CyberGreen else tabGlow
                                    )
                                }
                            }
                        }
                        
                        // Section 2: Stats Dashboard
                        Column(
                            modifier = Modifier.weight(1.4f).padding(start = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                filterCategory.uppercase(), 
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black, 
                                color = NeonCyan.copy(0.7f), 
                                letterSpacing = 2.sp
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Stat 1: Mission / Routine
                                Column {
                                    AnimatedContent(
                                        targetState = "$completedTasks/$totalTasks",
                                        transitionSpec = {
                                            fadeIn(tween(220)) togetherWith fadeOut(tween(90))
                                        },
                                        label = "taskCount"
                                    ) { target ->
                                        Text(
                                            target, 
                                            style = MaterialTheme.typography.titleLarge, 
                                            fontWeight = FontWeight.Black, 
                                            color = Color.White
                                        )
                                    }
                                    Text(
                                        if (selectedTab == 2) "RENCANA" else "RUTINITAS",
                                        fontSize = 9.sp, 
                                        color = Color.White.copy(0.4f), 
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Divider
                                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(0.1f)))

                                // Stat 2: Tanggungan / Konsistensi
                                Column(horizontalAlignment = Alignment.Start) {
                                    if (selectedTab == 2) {
                                        val load = filteredList
                                            .filter { !it.isDone }
                                            .sumOf { 
                                                when(it.priority) {
                                                    2 -> 3 // Tinggi
                                                    1 -> 2 // Sedang
                                                    else -> 1 // Rendah
                                                }
                                            }
                                        
                                        val (label, color, segments) = when {
                                            load > 10 -> Triple("PUSING", CyberRed, 5)
                                            load > 6 -> Triple("TINGGI", CyberAmber, 4)
                                            load > 3 -> Triple("SEDANG", Color.Yellow, 3)
                                            load > 0 -> Triple("AMAN", CyberGreen, 2)
                                            else -> Triple("SANTAI", NeonCyan, 1)
                                        }

                                        // Segmented Meter Visual
                                        Row(
                                            modifier = Modifier.padding(bottom = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            repeat(5) { index ->
                                                val isActive = index < segments
                                                Box(
                                                    modifier = Modifier
                                                        .width(4.dp)
                                                        .height(8.dp + (index * 2).dp)
                                                        .clip(RoundedCornerShape(1.dp))
                                                        .background(if (isActive) color else Color.White.copy(0.1f))
                                                )
                                            }
                                        }

                                        Text(
                                            label, 
                                            style = MaterialTheme.typography.labelSmall, 
                                            fontWeight = FontWeight.Black, 
                                            color = color,
                                            letterSpacing = 1.sp
                                        )
                                        Text("TANGGUNGAN", fontSize = 8.sp, color = Color.White.copy(0.4f), fontWeight = FontWeight.Bold)
                                    } else {
                                        // Dashboard Orbit Berulang: Konsistensi (Ritme)
                                        // Count tasks completed today for consistency visual
                                        val completedToday = filteredList.count { 
                                            it.completedAt != null && 
                                            SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(it.completedAt!!)) == 
                                            SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                                        }
                                        val displayStreak = if (totalTasks > 0 && completedToday == totalTasks) streak + 1 else streak
                                        
                                        val (label, color, segments) = when {
                                            displayStreak > 10 -> Triple("SEMPURNA", CyberGreen, 5)
                                            displayStreak > 5 -> Triple("STABIL", NeonCyan, 4)
                                            displayStreak > 2 -> Triple("LANCAR", Color.Yellow, 3)
                                            displayStreak > 0 -> Triple("MULAI", CyberAmber, 2)
                                            else -> Triple("PUTUS", Color.White.copy(0.3f), 1)
                                        }

                                        Row(
                                            modifier = Modifier.padding(bottom = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            repeat(5) { index ->
                                                val isActive = index < segments
                                                Box(
                                                    modifier = Modifier
                                                        .width(4.dp)
                                                        .height(8.dp + (index * 2).dp)
                                                        .clip(RoundedCornerShape(1.dp))
                                                        .background(if (isActive) color else Color.White.copy(0.1f))
                                                )
                                            }
                                        }

                                        Text(
                                            label, 
                                            style = MaterialTheme.typography.labelSmall, 
                                            fontWeight = FontWeight.Black, 
                                            color = color,
                                            letterSpacing = 1.sp
                                        )
                                        Text("KONSISTENSI", fontSize = 8.sp, color = Color.White.copy(0.4f), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Status Message with pulsing effect if 100%
                            Text(
                                when {
                                    progressAnim >= 1f -> "ORBIT AMAN ✓"
                                    selectedTab == 1 -> {
                                        when {
                                            progressAnim > 0.9f -> "SEDIKIT LAGI!"
                                            progressAnim > 0.8f -> "HAMPIR SAMPAI."
                                            progressAnim > 0.6f -> "SETENGAH PERJALANAN."
                                            progressAnim > 0.4f -> "MULAI SERIUS!"
                                            progressAnim > 0.2f -> "LANJUTKAN!"
                                            progressAnim > 0.1f -> "AWALAN BAGUS."
                                            else -> "MULAI AKTIVITAS!"
                                        }
                                    }
                                    else -> {
                                        when {
                                            progressAnim > 0.9f -> "SEDIKIT LAGI!"
                                            progressAnim > 0.8f -> "HAMPIR SAMPAI."
                                            progressAnim > 0.6f -> "SETENGAH PERJALANAN."
                                            progressAnim > 0.4f -> "MULAI SERIUS!"
                                            progressAnim > 0.2f -> "LANJUTKAN!"
                                            progressAnim > 0.1f -> "AWALAN BAGUS."
                                            else -> "MULAI AKTIVITAS!"
                                        }
                                    }
                                }, 
                                style = MaterialTheme.typography.labelSmall, 
                                color = if (progressAnim >= 1f) CyberGreen else Color.White.copy(0.5f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filterCategories) { cat ->
                        CategoryChip(
                            glowColor = glowColor,
                            category = cat,
                            selected = filterCategory == cat,
                            onClick = { filterCategory = cat }
                        )
                    }
                }

                if (filteredList.isEmpty()) {
                    CyberEmptyState(glowColor)
                } else {
                    val lazyListState = rememberLazyListState()
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(filteredList) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { startPos ->
                                        lazyListState.layoutInfo.visibleItemsInfo
                                            .firstOrNull { item ->
                                                startPos.y.toInt() in item.offset..(item.offset + item.size)
                                            }
                                            ?.let { it.key as? Int }
                                            ?.let { key ->
                                                draggedItemIndex = filteredList.indexOfFirst { it.id == key }
                                            }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        draggingOffset += dragAmount.y
                                        
                                        draggedItemIndex?.let { currentIndex ->
                                            val targetIndex = if (draggingOffset > 50f) currentIndex + 1 else if (draggingOffset < -50f) currentIndex - 1 else currentIndex
                                            if (targetIndex in filteredList.indices && targetIndex != currentIndex) {
                                                viewModel.moveTask(currentIndex, targetIndex, filteredList)
                                                draggedItemIndex = targetIndex
                                                draggingOffset = 0f
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        draggedItemIndex = null
                                        draggingOffset = 0f
                                    },
                                    onDragCancel = {
                                        draggedItemIndex = null
                                        draggingOffset = 0f
                                    }
                                )
                            },
                        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredList, key = { it.id }) { item ->
                            val isDragging = draggedItemIndex != null && filteredList.indexOf(item) == draggedItemIndex
                            val dragModifier = if (isDragging) {
                                Modifier
                                    .zIndex(1f)
                                    .graphicsLayer { translationY = draggingOffset }
                                    .shadow(16.dp, RoundedCornerShape(24.dp))
                            } else Modifier

                            Box(modifier = dragModifier.animateItem()) {
                                CyberTodoRow(
                                    glowColor = glowColor,
                                    item = item,
                                    onToggle = { viewModel.updateTaskStatus(item, !item.isDone) },
                                    onDelete = { viewModel.deleteTask(item) },
                                    onEdit = {
                                        editingItem = item
                                        showSheet = true
                                    },
                                    onPin = { viewModel.togglePin(item) },
                                    onViewDetail = { detailItem = item }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        CyberSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettings = false }
        )
    }

    if (showFlightLog) {
        CyberFlightLogDialog(
            todoList = todoList,
            onDismiss = { showFlightLog = false }
        )
    }

    if (showHelp) {
        CyberHelpDialog(onDismiss = { showHelp = false })
    }

    if (showSheet) {
        CyberAddEditSheet(
            glowColor = glowColor,
            item = editingItem,
            sheetState = sheetState,
            categories = categories,
            initialTab = selectedTab,
            onDismiss = { showSheet = false },
            onSave = { text, desc, p, c, e, d, t, r, l, s ->
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        if (editingItem == null) {
                            viewModel.addTask(text, desc, p, c, e, d, t, r, l, s)
                        } else {
                            viewModel.updateTask(editingItem!!.copy(
                                task = text,
                                description = desc,
                                priority = p,
                                category = c,
                                emoji = e,
                                dueDate = d,
                                dueTime = t,
                                repeatMode = r,
                                attachmentLink = l,
                                soundUri = s
                            ))
                        }
                        showSheet = false
                    }
                }
            }
        )
    }

    if (detailItem != null) {
        CyberDetailDialog(
            glowColor = glowColor,
            item = detailItem!!,
            onDismiss = { detailItem = null },
            onEdit = {
                editingItem = detailItem
                detailItem = null
                showSheet = true
            }
        )
    }
}

class NoHapticFeedback : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        // Do nothing to suppress vibration
    }
}

@Composable
fun CyberDetailDialog(
    glowColor: Color,
    item: TodoItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        confirmButton = {},
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .clip(RoundedCornerShape(32.dp))
            .border(1.dp, glowColor.copy(0.3f), RoundedCornerShape(32.dp)),
        containerColor = Color(0xFF0E1421),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(0.05f))
                            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.emoji, fontSize = 28.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            item.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            item.task,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (item.dueDate != null || item.dueTime != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, null, tint = glowColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        val dateStr = item.dueDate?.let {
                            val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
                            sdf.format(Date(it))
                        } ?: "Tidak ada tanggal"
                        Text(
                            "$dateStr ${item.dueTime ?: ""}",
                            color = Color.White.copy(0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (item.repeatMode != "None") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Berulang: ${if(item.repeatMode == "Daily") "Harian" else "Mingguan"}", color = NeonCyan, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (item.attachmentLink.isNotBlank()) {
                    val localContext = LocalContext.current
                    Surface(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.attachmentLink))
                                localContext.startActivity(intent)
                            } catch (e: Exception) {
                                // Handle invalid URL
                            }
                        },
                        color = NeonCyan.copy(0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Link, null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Buka Lampiran", color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Surface(
                    color = Color.White.copy(0.03f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "DESKRIPSI MISI",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(0.4f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (item.description.isNotBlank()) item.description else "Tidak ada deskripsi tambahan untuk misi ini.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(0.9f),
                            lineHeight = 24.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(0.2f))
                    ) {
                        Text("TUTUP", color = Color.White)
                    }
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = glowColor)
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EDIT MISI")
                    }
                }
            }
        }
    )
}

@Composable
fun CyberSettingsDialog(
    viewModel: TodoViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("OrbitListPrefs", Context.MODE_PRIVATE)
    var currentSoundUri by remember { mutableStateOf(prefs.getString("global_sound_uri", null)) }
    val categories by viewModel.categories.collectAsState()
    var newCategoryName by remember { mutableStateOf("") }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                uri?.let {
                    prefs.edit().putString("global_sound_uri", it.toString()).apply()
                    currentSoundUri = it.toString()
                }
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                onDismiss()
            }, colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)) {
                Text("Tutup", color = Color(0xFF1A0B2E))
            }
        },
        containerColor = Color(0xFF0E1421),
        title = { Text("PENGATURAN", color = Color.White, fontWeight = FontWeight.Black) },
        text = {
            Column {
                Text("NADA DERING", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Pilih Suara Notifikasi")
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentSoundUri?.let { Uri.parse(it) })
                        }
                        ringtoneLauncher.launch(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.05f))
                ) {
                    Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (currentSoundUri == null) "Pilih Nada Dering" else "Nada Dering Terpilih")
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("BAGIKAN APLIKASI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Kelola tugasmu dalam orbit yang teratur dengan OrbitList! Kunjungi: https://orbitlistapp.vercel.app/")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Bagikan OrbitList"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(0.1f))
                ) {
                    Icon(Icons.Default.Share, null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rekomendasikan ke Teman", color = NeonCyan)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("MANAJEMEN KATEGORI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = { Text("Nama Kategori...", fontSize = 12.sp, color = Color.White.copy(0.3f)) },
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(0.05f),
                            unfocusedContainerColor = Color.White.copy(0.05f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                viewModel.addCategory(newCategoryName)
                                newCategoryName = ""
                            }
                        },
                        modifier = Modifier.background(NeonCyan, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color(0xFF050B18))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                // Horizontal list of deletable categories
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        Surface(
                            color = Color.White.copy(0.05f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color.White.copy(0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cat, fontSize = 11.sp, color = Color.White)
                                if (categories.size > 1) {
                                    IconButton(
                                        onClick = { viewModel.removeCategory(cat) },
                                        modifier = Modifier.size(24.dp).padding(start = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = CyberRed, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun CyberHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)) {
                Text("Paham, Komandan!", color = Color(0xFF050B18), fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF0E1421),
        modifier = Modifier.clip(RoundedCornerShape(24.dp)).border(1.dp, NeonCyan.copy(0.2f), RoundedCornerShape(24.dp)),
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.RocketLaunch, null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(12.dp))
                Text("MANUAL OPERASI LENGKAP", color = Color.White, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("NAVIGASI & STRUKTUR GALAKSI", fontWeight = FontWeight.Bold, color = NeonCyan, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                HelpSection("📅 Tab Koordinat", "OrbitList membagi tugas dalam 3 zona: 'Harian' (rutinitas harian), 'Mingguan' (rutinitas mingguan), dan 'Agenda' (misi sekali jalan/tugas umum).")
                HelpSection("🎨 Identitas Visual", "Setiap zona memiliki warna unik: Kuning (Harian), Merah (Mingguan), dan Ungu (Agenda). Gunakan warna ini sebagai navigasi cepat mata Anda.")
                HelpSection("🌐 Akses Ekosistem", "Ikon 'Web' di ujung kanan navbar menghubungkan Anda ke situs pusat OrbitList untuk informasi ekosistem aplikasi yang lebih luas.")

                Spacer(modifier = Modifier.height(16.dp))
                Text("MEKANISME ORBIT BERULANG", fontWeight = FontWeight.Bold, color = NeonCyan, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                HelpSection("🔄 Sistem Loop Tunggal", "Tugas Harian/Mingguan tidak akan bertumpuk. Begitu selesai dikonfirmasi, tugas tersebut akan otomatis menjadwalkan dirinya sendiri ke hari/minggu berikutnya.")
                HelpSection("🔒 Ikon Gembok", "Jika muncul ikon gembok, berarti orbit waktu belum tiba. Anda belum bisa menyelesaikan tugas tersebut sampai waktu yang ditentukan.")
                HelpSection("📈 Progres & Ritme", "Indikator persentase di Tab Harian/Mingguan disebut 'Ritme'. Ini tetap menghitung keberhasilan Anda hari ini meskipun tugas sudah berpindah tanggal ke besok.")

                Spacer(modifier = Modifier.height(16.dp))
                Text("DASHBOARD & ANALITIK", fontWeight = FontWeight.Bold, color = NeonCyan, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                HelpSection("⚖️ Meteran Tanggungan", "Di Tab Agenda, ada indikator beban kerja. Semakin banyak tugas prioritas 'Tinggi', meteran akan berubah dari 'Aman' menjadi 'Pusing' (Merah).")
                HelpSection("🔥 Konsistensi (Streak)", "Di Tab Rutinitas, ada meteran konsistensi. Jika Anda menyelesaikan semua rutinitas harian tanpa putus, angka streak di dashboard akan terus bertambah.")
                HelpSection("📊 Log Penerbangan", "Gunakan ikon grafik untuk melihat pembagian beban tugas Anda. Grafik ini membantu Anda melihat apakah hidup Anda terlalu berat di Agenda atau Rutinitas.")

                Spacer(modifier = Modifier.height(16.dp))
                Text("MANAJEMEN MISI TINGKAT LANJUT", fontWeight = FontWeight.Bold, color = NeonCyan, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                HelpSection("🏷️ Kategori Kustom", "Anda bisa menambah atau menghapus kategori misi (seperti: Belajar, Gym, dll) melalui menu Pengaturan (ikon gerigi).")
                HelpSection("📎 Lampiran & Deskripsi", "Setiap misi bisa menampung link URL (seperti dokumen atau meeting) dan deskripsi panjang. Cek bagian 'Info' pada menu aksi.")
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("NOTIFIKASI & ALARM", fontWeight = FontWeight.Bold, color = NeonCyan, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                HelpSection("🔔 Pengingat Orbit", "Aplikasi akan mengirimkan notifikasi saat waktu tugas tiba. Anda bisa mengatur nada dering khusus di menu Pengaturan agar tidak terlewat.")
            }
        }
    )
}

@Composable
fun HelpSection(title: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(title, fontWeight = FontWeight.Bold, color = NeonCyan, fontSize = 14.sp)
        Text(desc, color = Color.White.copy(0.7f), fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
fun CyberFlightLogDialog(
    todoList: List<TodoItem>,
    onDismiss: () -> Unit
) {
    val completedThisWeek = todoList.count { 
        it.isDone && it.completedAt != null && 
        it.completedAt!! > System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000 
    }
    val totalThisWeek = todoList.count { 
        it.createdAt > System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000 
    }
    
    val harianMissions = todoList.filter { it.repeatMode == "Daily" }
    val mingguanMissions = todoList.filter { it.repeatMode == "Weekly" }
    val agendaMissions = todoList.filter { it.repeatMode == "None" }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        containerColor = Color(0xFF0E1421),
        modifier = Modifier.clip(RoundedCornerShape(24.dp)).border(1.dp, NeonCyan.copy(0.2f), RoundedCornerShape(24.dp)),
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.RocketLaunch, null, tint = NeonCyan, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("LOG PENERBANGAN", fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp)
                Text("Statistik Perjalanan Galaksi", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.5f))
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Mini Bar Chart - Task 5 Split
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val maxVal = maxOf(harianMissions.size, mingguanMissions.size, agendaMissions.size, 5)
                    ChartBar("Harian", harianMissions.size, maxVal, Color(0xFFFFD700))
                    ChartBar("Mingguan", mingguanMissions.size, maxVal, Color(0xFFFF0000))
                    ChartBar("Agenda", agendaMissions.size, maxVal, Color(0xFF8A2BE2))
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem("TOTAL SELESAI", todoList.count { it.isDone }.toString())
                    StatItem("DALAM ORBIT", todoList.count { !it.isDone }.toString())
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val highPriorityCount = todoList.count { !it.isDone && it.priority == 2 }
                if (highPriorityCount > 0) {
                    Text(
                        "⚠️ $highPriorityCount MISI KRITIS TERDETEKSI", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = CyberRed,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                GlassCard(modifier = Modifier.padding(8.dp), glowColor = NeonCyan.copy(0.1f)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("EFISIENSI MINGGU INI", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                        Text("$completedThisWeek / $totalThisWeek", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Black)
                        LinearProgressIndicator(
                            progress = if (totalThisWeek > 0) completedThisWeek.toFloat() / totalThisWeek else 0f,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(8.dp).clip(CircleShape),
                            color = NeonCyan,
                            trackColor = Color.White.copy(0.1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = NeonCyan), shape = RoundedCornerShape(12.dp)) {
                    Text("KEMBALI KE ORBIT", color = Color(0xFF050B18), fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

@Composable
fun ChartBar(label: String, value: Int, maxOfAll: Int, color: Color) {
    val heightAnim by animateFloatAsState(
        targetValue = if (maxOfAll > 0) value.toFloat() / maxOfAll else 0f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "chartHeight"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Text(
            value.toString(), 
            color = Color.White, 
            fontSize = 12.sp, 
            fontWeight = FontWeight.Black
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Fixed container for the bar area
        Box(
            modifier = Modifier
                .height(100.dp)
                .width(40.dp)
                .background(Color.White.copy(0.05f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(heightAnim.coerceIn(0.05f, 1f))
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(color, color.copy(0.6f))
                        )
                    )
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            label, 
            color = Color.White.copy(0.6f), 
            fontSize = 10.sp, 
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.4f))
        Text(value, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Black)
    }
}
@Composable
fun CategoryChip(glowColor: Color, category: String, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1.1f else 1f)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) glowColor else GlassSurface,
        modifier = Modifier.scale(scale).shadow(if (selected) 8.dp else 0.dp, RoundedCornerShape(12.dp))
    ) {
        Text(
            category, 
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else Color.White.copy(0.7f),
            fontSize = 11.sp
        )
    }
}

@Composable
fun InteractiveFAB(glowColor: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "fabScale")
    
    Box(
        modifier = Modifier
            .padding(16.dp)
            .scale(scale)
            .size(64.dp)
            .shadow(
                elevation = 20.dp, 
                shape = CircleShape, 
                spotColor = glowColor,
                ambientColor = glowColor
            )
            .background(
                Brush.linearGradient(
                    listOf(glowColor, VividViolet, HotPink),
                    start = Offset(0f, 0f),
                    end = Offset(200f, 200f)
                ), 
                CircleShape
            )
            .clickable(
                interactionSource = interactionSource, 
                indication = null, 
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Add, 
            contentDescription = null, 
            tint = Color.White, 
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun CyberTodoRow(glowColor: Color, item: TodoItem, onToggle: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit, onPin: () -> Unit, onViewDetail: () -> Unit) {
    val pColor = when(item.priority) {
        2 -> CyberRed
        1 -> CyberAmber
        else -> CyberGreen
    }
    
    val isRecurring = item.repeatMode != "None"
    val isDue = remember(item.dueDate, item.dueTime, item.isDone) {
        if (item.isDone) return@remember true // If already done, we show checklist, not lock

        val now = Calendar.getInstance()
        val target = Calendar.getInstance()
        if (item.dueDate != null) {
            target.timeInMillis = item.dueDate
            if (item.dueTime != null) {
                val parts = item.dueTime.split(":")
                target.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                target.set(Calendar.MINUTE, parts[1].toInt())
                target.set(Calendar.SECOND, 0)
            } else {
                target.set(Calendar.HOUR_OF_DAY, 0)
                target.set(Calendar.MINUTE, 0)
                target.set(Calendar.SECOND, 0)
            }
            now.timeInMillis >= target.timeInMillis
        } else true
    }

    var isExtending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val alpha by animateFloatAsState(if (item.isDone || isExtending) 0.5f else 1f, label = "alpha")
    val scale by animateFloatAsState(if (item.isDone || isExtending) 0.98f else 1f, label = "scale")
    var isExpanded by remember { mutableStateOf(false) }
    
    Surface(
        onClick = { isExpanded = !isExpanded },
        shape = RoundedCornerShape(24.dp),
        color = if (item.isDone || isExtending) Color.White.copy(0.02f) else GlassSurface.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .border(
                1.dp, 
                if (item.isPinned) glowColor.copy(0.4f) else Color.White.copy(0.08f), 
                RoundedCornerShape(24.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp).alpha(alpha)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(0.05f))
                        .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.emoji, fontSize = 22.sp)
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.isPinned) {
                            Icon(Icons.Filled.PushPin, "Pinned", modifier = Modifier.size(14.dp), tint = glowColor)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            item.task, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 17.sp, 
                            color = Color.White,
                            style = if (item.isDone || isExtending) MaterialTheme.typography.bodyLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else MaterialTheme.typography.bodyLarge
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Surface(
                            color = pColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                item.category.uppercase(), 
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp, 
                                fontWeight = FontWeight.Black, 
                                color = pColor,
                                letterSpacing = 0.5.sp
                            )
                        }

                        if (isRecurring) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(12.dp), tint = if(isDue) glowColor else NeonCyan)
                        }

                        if (item.attachmentLink.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Link, null, modifier = Modifier.size(12.dp), tint = NeonCyan)
                        }
                        
                        if (item.dueDate != null || item.dueTime != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Schedule, null, modifier = Modifier.size(12.dp), tint = if(isRecurring && !isDue) NeonCyan else Color.White.copy(0.4f))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = buildString {
                                    if (item.dueDate != null) {
                                        val sdf = SimpleDateFormat("EE, dd MMM", Locale.getDefault())
                                        append(sdf.format(Date(item.dueDate)))
                                    }
                                    if (item.dueTime != null) {
                                        if (isNotEmpty()) append(" • ")
                                        append(item.dueTime)
                                    }
                                },
                                fontSize = 11.sp, 
                                color = if(isRecurring && !isDue) NeonCyan else Color.White.copy(0.4f),
                                fontWeight = if(isRecurring && !isDue) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                if (!isRecurring || isDue || item.isDone) {
                    IconButton(
                        onClick = {
                            if (isRecurring) {
                                // If recurring, can only check, cannot uncheck once done
                                if (!item.isDone && !isExtending) {
                                    isExtending = true
                                    scope.launch {
                                        kotlinx.coroutines.delay(600)
                                        onToggle()
                                        isExtending = false
                                    }
                                }
                            } else {
                                // One-time task can be toggled
                                onToggle()
                            }
                        }, 
                        modifier = Modifier.size(36.dp),
                        enabled = !isRecurring || !item.isDone
                    ) {
                        val checkScale by animateFloatAsState(
                            targetValue = if (item.isDone || isExtending) 1.2f else 1f,
                            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                        )
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .scale(checkScale)
                                .clip(CircleShape)
                                .border(2.dp, if (item.isDone || isExtending) pColor else Color.White.copy(0.2f), CircleShape)
                                .background(if (item.isDone || isExtending) pColor else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.isDone || isExtending) Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = SpaceDark)
                            else if (isRecurring) Icon(Icons.Default.Autorenew, null, modifier = Modifier.size(14.dp), tint = Color.White.copy(0.4f))
                        }
                    }
                } else {
                    // Task is recurring but not due yet
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LockClock, 
                            null, 
                            tint = Color.White.copy(0.15f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.White.copy(0.05f))
                    
                    // Action Buttons Row (Placed Above for better access)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp), 
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Aksi:", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = Color.White.copy(0.3f),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        IconButton(onClick = onViewDetail) {
                            Icon(Icons.Default.Info, null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onPin) {
                            Icon(if (item.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, null, tint = if (item.isPinned) glowColor else Color.White.copy(0.3f), modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.DeleteOutline, null, tint = CyberRed.copy(0.6f), modifier = Modifier.size(18.dp))
                        }
                    }

                    // Status Text (Placed Below to avoid push-out)
                    if (isRecurring) {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.CenterEnd) {
                            if (!isDue) {
                                Text(
                                    "Orbit belum tiba di koordinat waktu.", 
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan.copy(0.5f),
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.End
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ErrorOutline, null, tint = glowColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Orbit akan terupdate otomatis kemudian.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = glowColor,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.End
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
fun GlassCard(
    modifier: Modifier = Modifier,
    glowColor: Color = ElectricIndigo.copy(alpha = 0.1f),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = glowColor,
                ambientColor = glowColor
            )
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(0.08f),
                        Color.White.copy(0.02f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(0.2f),
                        Color.White.copy(0.05f)
                    )
                ),
                RoundedCornerShape(32.dp)
            )
    ) {
        content()
    }
}

@Composable
fun CyberEmptyState(glowColor: Color) {
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(150.dp)) {
                drawCircle(glowColor.copy(0.1f))
            }
            Icon(Icons.Default.RocketLaunch, null, modifier = Modifier.size(60.dp), tint = glowColor)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("TIDAK ADA MISI", fontWeight = FontWeight.Black, letterSpacing = 4.sp, color = Color.White)
        Text("Menunggu misi selanjutnya...", color = Color.White.copy(0.5f), fontSize = 12.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberAddEditSheet(
    glowColor: Color,
    item: TodoItem?,
    sheetState: SheetState,
    categories: List<String>,
    initialTab: Int = 0,
    onDismiss: () -> Unit, 
    onSave: (String, String, Int, String, String, Long?, String?, String, String, String?) -> Unit
) {
    var text by remember { mutableStateOf(item?.task ?: "") }
    var description by remember { mutableStateOf(item?.description ?: "") }
    var priority by remember { mutableIntStateOf(item?.priority ?: 1) }
    var category by remember { mutableStateOf(item?.category ?: if (categories.isNotEmpty()) categories[0] else "Umum") }
    var emoji by remember { mutableStateOf(item?.emoji ?: "📝") }
    var date by remember { mutableStateOf(item?.dueDate) }
    var time by remember { mutableStateOf(item?.dueTime) }
    var repeatMode by remember { 
        mutableStateOf(
            item?.repeatMode ?: when(initialTab) {
                1 -> "Daily"
                2 -> "Weekly"
                else -> "None"
            }
        ) 
    }
    var attachmentLink by remember { mutableStateOf(item?.attachmentLink ?: "") }
    
    val daysOfWeek = listOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
    var selectedDayIndex by remember { 
        mutableIntStateOf(
            item?.dueDate?.let {
                val cal = Calendar.getInstance()
                cal.timeInMillis = it
                cal.get(Calendar.DAY_OF_WEEK) - 1
            } ?: Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
        )
    }

    val context = LocalContext.current
    
    val emojiOptions = listOf(
        "📝", "💻", "📚", "🎓", "☕", 
        "🧪", "🧬", "🎨", "🎭", "🎼", 
        "🏀", "⚽", "🏃", "🧘", "🍕", 
        "🍔", "🚌", "🏠", "🛒", "💰", 
        "📅", "⏰", "💡", "🔥", "🚀", "❤️"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0E1421),
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (item == null) "MISI BARU" else "PERBARUI MISI", 
                    fontWeight = FontWeight.Black, 
                    letterSpacing = 2.sp, 
                    color = Color.White
                )
                
                Surface(
                    color = if (repeatMode == "None") NeonCyan.copy(0.1f) else glowColor.copy(0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (repeatMode == "None") NeonCyan.copy(0.3f) else glowColor.copy(0.3f))
                ) {
                    Text(
                        if (repeatMode == "None") "AGENDA" else "ORBIT BERULANG",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (repeatMode == "None") NeonCyan else glowColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            val focusManager = LocalFocusManager.current

            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Nama tugas...", color = Color.White.copy(0.3f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(0.05f),
                    unfocusedContainerColor = Color.White.copy(0.05f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = NeonCyan,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Detail tambahan...", color = Color.White.copy(0.3f)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.White.copy(0.1f),
                    focusedBorderColor = glowColor.copy(0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        if (repeatMode == "None") {
                            val calendar = Calendar.getInstance()
                            if (date != null) calendar.timeInMillis = date!!
                            DatePickerDialog(context, { _, y, m, d ->
                                date = Calendar.getInstance().apply { set(y, m, d) }.timeInMillis
                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.05f)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = repeatMode == "None"
                ) {
                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp), tint = if(repeatMode == "None") Color.White else Color.White.copy(0.2f))
                    Spacer(modifier = Modifier.width(8.dp))
                    when (repeatMode) {
                        "None" -> {
                            val sdf = SimpleDateFormat("EEEE, dd/MM", Locale("id", "ID"))
                            Text(if (date == null) "Pilih Tanggal" else sdf.format(Date(date!!)), fontSize = 10.sp)
                        }
                        "Daily" -> {
                            Text("Setiap Hari", fontSize = 10.sp, color = Color.White.copy(0.5f))
                        }
                        "Weekly" -> {
                            Text("Setiap ${daysOfWeek[selectedDayIndex]}", fontSize = 10.sp, color = Color.White.copy(0.5f))
                        }
                    }
                }
                
                Button(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        val initialHour = time?.split(":")?.get(0)?.toInt() ?: calendar.get(Calendar.HOUR_OF_DAY)
                        val initialMin = time?.split(":")?.get(1)?.toInt() ?: calendar.get(Calendar.MINUTE)
                        
                        TimePickerDialog(context, { _, h, m ->
                            time = String.format("%02d:%02d", h, m)
                            
                            if (repeatMode == "Daily") {
                                val cal = Calendar.getInstance()
                                if (h < cal.get(Calendar.HOUR_OF_DAY) || (h == cal.get(Calendar.HOUR_OF_DAY) && m <= cal.get(Calendar.MINUTE))) {
                                    cal.add(Calendar.DAY_OF_YEAR, 1)
                                }
                                date = cal.timeInMillis
                            }
                        }, initialHour, initialMin, true).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(time ?: "Pilih Jam", fontSize = 10.sp)
                }
            }

            if (repeatMode == "Weekly") {
                Spacer(modifier = Modifier.height(16.dp))
                Text("PILIH HARI ORBIT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White.copy(0.4f))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                    daysOfWeek.forEachIndexed { index, day ->
                        val active = selectedDayIndex == index
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) glowColor.copy(0.2f) else Color.White.copy(0.05f))
                                .border(1.dp, if (active) glowColor else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { 
                                    selectedDayIndex = index
                                    val cal = Calendar.getInstance()
                                    val currentDay = cal.get(Calendar.DAY_OF_WEEK) - 1
                                    
                                    var daysUntil = index - currentDay
                                    if (daysUntil < 0) daysUntil += 7
                                    
                                    if (daysUntil == 0 && time != null) {
                                        val now = Calendar.getInstance()
                                        val h = time!!.split(":")[0].toInt()
                                        val m = time!!.split(":")[1].toInt()
                                        if (h < now.get(Calendar.HOUR_OF_DAY) || (h == now.get(Calendar.HOUR_OF_DAY) && m <= now.get(Calendar.MINUTE))) {
                                            daysUntil = 7
                                        }
                                    }

                                    cal.add(Calendar.DAY_OF_YEAR, daysUntil)
                                    date = cal.timeInMillis
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(day, fontSize = 11.sp, color = if (active) Color.White else Color.White.copy(0.5f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("PENGULANGAN", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White.copy(0.4f))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                listOf("None" to "Agenda", "Daily" to "Harian", "Weekly" to "Mingguan").forEach { (mode, label) ->
                    val active = repeatMode == mode
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) (if(mode=="None") NeonCyan else glowColor).copy(0.2f) else Color.White.copy(0.05f))
                            .border(1.dp, if (active) (if(mode=="None") NeonCyan else glowColor) else Color.White.copy(0.1f), RoundedCornerShape(8.dp))
                            .clickable { 
                                repeatMode = mode 
                                if (mode == "Daily") {
                                    val cal = Calendar.getInstance()
                                    time?.let { t ->
                                        val h = t.split(":")[0].toInt()
                                        val m = t.split(":")[1].toInt()
                                        if (h < cal.get(Calendar.HOUR_OF_DAY) || (h == cal.get(Calendar.HOUR_OF_DAY) && m <= cal.get(Calendar.MINUTE))) {
                                            cal.add(Calendar.DAY_OF_YEAR, 1)
                                        }
                                    }
                                    date = cal.timeInMillis
                                } else if (mode == "Weekly") {
                                    val cal = Calendar.getInstance()
                                    val currentDay = cal.get(Calendar.DAY_OF_WEEK) - 1
                                    var daysUntil = selectedDayIndex - currentDay
                                    if (daysUntil < 0) daysUntil += 7
                                    if (daysUntil == 0 && time != null) {
                                        val now = Calendar.getInstance()
                                        val h = time!!.split(":")[0].toInt()
                                        val m = time!!.split(":")[1].toInt()
                                        if (h < now.get(Calendar.HOUR_OF_DAY) || (h == now.get(Calendar.HOUR_OF_DAY) && m <= now.get(Calendar.MINUTE))) {
                                            daysUntil = 7
                                        }
                                    }
                                    cal.add(Calendar.DAY_OF_YEAR, daysUntil)
                                    date = cal.timeInMillis
                                } else {
                                    date = null // Reset for "None" if desired, or keep previous
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(label, fontSize = 12.sp, color = if (active) Color.White else Color.White.copy(0.5f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("EMOJI", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White.copy(0.4f))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                emojiOptions.forEach { e ->
                    val active = emoji == e
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) glowColor.copy(0.2f) else Color.White.copy(0.05f))
                            .border(1.dp, if (active) glowColor else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { emoji = e },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(e, fontSize = 20.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("KATEGORI", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White.copy(0.4f))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    CategoryChip(
                        glowColor = glowColor,
                        category = cat,
                        selected = category == cat,
                        onClick = { category = cat }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("PRIORITAS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White.copy(0.4f))
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("RENDAH" to 0, "SEDANG" to 1, "TINGGI" to 2).forEach { (label, value) ->
                    val active = priority == value
                    val pColor = when(value) {
                        2 -> CyberRed
                        1 -> CyberAmber
                        else -> CyberGreen
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) pColor.copy(0.2f) else Color.White.copy(0.05f))
                            .border(1.dp, if (active) pColor else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { priority = value },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontWeight = FontWeight.Black, color = if (active) pColor else Color.White.copy(0.4f), fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = attachmentLink,
                onValueChange = { attachmentLink = it },
                placeholder = { Text("Link lampiran (URL)...", color = Color.White.copy(0.3f)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Link, null, tint = NeonCyan) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.White.copy(0.1f),
                    focusedBorderColor = NeonCyan.copy(0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { onSave(text, description, priority, category, emoji, date, time, repeatMode, attachmentLink, null) },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if(repeatMode == "None") NeonCyan else glowColor,
                    disabledContainerColor = (if(repeatMode == "None") NeonCyan else glowColor).copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (item == null) "EKSEKUSI" else "SINKRONISASI", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
        }
    }
}
