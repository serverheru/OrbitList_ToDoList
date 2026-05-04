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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = context.getSharedPreferences("OrbitListPrefs", Context.MODE_PRIVATE)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TodoApp(
    themeColors: List<Color>,
    glowColor: Color,
    viewModel: TodoViewModel = viewModel()
) {
    var editingItem by remember { mutableStateOf<TodoItem?>(null) }
    var detailItem by remember { mutableStateOf<TodoItem?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var showFlightLog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Misi Biasa, 1: Misi Berulang
    var filterCategory by remember { mutableStateOf("Semua") }
    val searchQuery by viewModel.searchQuery.collectAsState(initial = "")
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    
    val todoList by viewModel.allTasks.collectAsState(initial = emptyList())
    val categories = listOf("Umum", "Kerja", "Belajar", "Pribadi")
    val filterCategories = listOf("Semua") + categories
    
    val filteredList = todoList.filter { 
        val matchesCategory = filterCategory == "Semua" || it.category == filterCategory
        val isRecurring = it.repeatMode != "None"
        val matchesTab = if (selectedTab == 0) !isRecurring else isRecurring
        matchesCategory && matchesTab
    }

    // State untuk drag-and-drop
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffset by remember { mutableStateOf(0f) }

    val totalTasks = filteredList.size
    val completedTasks = filteredList.count { it.isDone }
    val progressAnim by animateFloatAsState(
        targetValue = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "progress"
    )

    Box(modifier = Modifier
        .fillMaxSize()
        .background(SpaceDark)
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
                                    "ORBITLIST",
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 4.sp,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Text(
                                    "Kelola tugas harianmu dalam orbit yang teratur.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(0.6f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { showFlightLog = true }) {
                                Icon(Icons.Default.QueryStats, "Log Penerbangan", tint = NeonCyan)
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
                NavigationBar(
                    containerColor = SpaceDark.copy(0.9f),
                    contentColor = Color.White
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.RocketLaunch, null) },
                        label = { Text("Misi Utama", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            unselectedIconColor = Color.White.copy(0.4f),
                            selectedTextColor = NeonCyan,
                            unselectedTextColor = Color.White.copy(0.4f),
                            indicatorColor = NeonCyan.copy(0.1f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Loop, null) },
                        label = { Text("Orbit Berulang", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = glowColor,
                            unselectedIconColor = Color.White.copy(0.4f),
                            selectedTextColor = glowColor,
                            unselectedTextColor = Color.White.copy(0.4f),
                            indicatorColor = glowColor.copy(0.1f)
                        )
                    )
                    val context = LocalContext.current
                    NavigationBarItem(
                        selected = false,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://orbitlistapp.vercel.app/"))
                            context.startActivity(intent)
                        },
                        icon = { Icon(Icons.Default.Link, null) },
                        label = { Text("Info Web", fontSize = 10.sp) },
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
                    glowColor = if (progressAnim > 0.8f) CyberGreen.copy(0.2f) else glowColor.copy(0.2f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
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
                                        if (progressAnim > 0.8f) listOf(CyberGreen, NeonCyan, CyberGreen)
                                        else listOf(glowColor, NeonCyan, glowColor)
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
                                    "SIAP", 
                                    fontSize = 9.sp, 
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp, 
                                    color = if (progressAnim > 0.8f) CyberGreen else NeonCyan
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(40.dp))
                        
                        Column {
                            Text(
                                filterCategory.uppercase(), 
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black, 
                                color = NeonCyan, 
                                letterSpacing = 2.sp
                            )
                            Text(
                                "$completedTasks / $totalTasks", 
                                style = MaterialTheme.typography.headlineMedium, 
                                fontWeight = FontWeight.Black, 
                                color = Color.White
                            )
                            Text(
                                when {
                                    progressAnim >= 1f -> "MISI SELESAI!"
                                    progressAnim > 0.8f -> "HAMPIR TIBA"
                                    progressAnim > 0.5f -> "SETENGAH JALAN"
                                    progressAnim > 0.2f -> "DALAM ORBIT"
                                    else -> "MULAI MISI"
                                }, 
                                style = MaterialTheme.typography.bodySmall, 
                                color = Color.White.copy(0.5f)
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
        CyberSettingsDialog(onDismiss = { showSettings = false })
    }

    if (showFlightLog) {
        CyberFlightLogDialog(
            todoList = todoList,
            onDismiss = { showFlightLog = false }
        )
    }

    if (showSheet) {
        CyberAddEditSheet(
            glowColor = glowColor,
            item = editingItem,
            sheetState = sheetState,
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
        containerColor = SpaceDark,
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
fun CyberSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("OrbitListPrefs", Context.MODE_PRIVATE)
    var currentSoundUri by remember { mutableStateOf(prefs.getString("global_sound_uri", null)) }
    var currentLanguage by remember { mutableStateOf(prefs.getString("global_lang", "Indonesia")) }
    var currentTheme by remember { mutableStateOf(prefs.getString("global_theme", "Default")) }

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
                Text("Tutup", color = SpaceDark)
            }
        },
        containerColor = SpaceDark,
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
            }
        }
    )
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
    
    val regularMissions = todoList.filter { it.repeatMode == "None" }
    val recurringMissions = todoList.filter { it.repeatMode != "None" }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        containerColor = SpaceDark,
        modifier = Modifier.clip(RoundedCornerShape(24.dp)).border(1.dp, NeonCyan.copy(0.2f), RoundedCornerShape(24.dp)),
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.RocketLaunch, null, tint = NeonCyan, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("LOG PENERBANGAN", fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp)
                Text("Statistik Perjalanan Galaksi", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.5f))
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Mini Bar Chart
                Row(
                    modifier = Modifier.fillMaxWidth().height(100.dp).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val maxVal = maxOf(regularMissions.size, recurringMissions.size, 1)
                    ChartBar("Misi Biasa", regularMissions.size, maxVal, NeonCyan)
                    ChartBar("Misi Berulang", recurringMissions.size, maxVal, ElectricIndigo)
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem("TOTAL SELESAI", todoList.count { it.isDone }.toString())
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
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
                    Text("KEMBALI KE ORBIT", color = SpaceDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

@Composable
fun ChartBar(label: String, value: Int, maxVal: Int, color: Color) {
    val heightFactor = value.toFloat() / maxVal
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier
                .width(40.dp)
                .fillMaxHeight(heightFactor.coerceAtLeast(0.1f))
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(color)
        )
        Text(label, color = Color.White.copy(0.5f), fontSize = 8.sp, modifier = Modifier.padding(top = 4.dp))
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
    
    val alpha by animateFloatAsState(if (item.isDone) 0.5f else 1f, label = "alpha")
    val scale by animateFloatAsState(if (item.isDone) 0.98f else 1f, label = "scale")
    var isExpanded by remember { mutableStateOf(false) }
    
    Surface(
        onClick = { isExpanded = !isExpanded },
        shape = RoundedCornerShape(24.dp),
        color = if (item.isDone) Color.White.copy(0.02f) else GlassSurface.copy(alpha = 0.4f),
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
                            style = if (item.isDone) MaterialTheme.typography.bodyLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else MaterialTheme.typography.bodyLarge
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

                        if (item.repeatMode != "None") {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(12.dp), tint = NeonCyan)
                        }

                        if (item.attachmentLink.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Link, null, modifier = Modifier.size(12.dp), tint = NeonCyan)
                        }
                        
                        if (item.dueDate != null || item.dueTime != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Schedule, null, modifier = Modifier.size(12.dp), tint = Color.White.copy(0.4f))
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
                                color = Color.White.copy(0.4f)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggle, 
                    modifier = Modifier.size(36.dp)
                ) {
                    val checkScale by animateFloatAsState(if (item.isDone) 1.2f else 1f)
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .scale(checkScale)
                            .clip(CircleShape)
                            .border(2.dp, if (item.isDone) pColor else Color.White.copy(0.2f), CircleShape)
                            .background(if (item.isDone) pColor else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.isDone) Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = SpaceDark)
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
        Text("STASI BERSIH", fontWeight = FontWeight.Black, letterSpacing = 4.sp, color = Color.White)
        Text("Menunggu misi selanjutnya...", color = Color.White.copy(0.5f), fontSize = 12.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberAddEditSheet(
    glowColor: Color,
    item: TodoItem?,
    sheetState: SheetState,
    onDismiss: () -> Unit, 
    onSave: (String, String, Int, String, String, Long?, String?, String, String, String?) -> Unit
) {
    var text by remember { mutableStateOf(item?.task ?: "") }
    var description by remember { mutableStateOf(item?.description ?: "") }
    var priority by remember { mutableIntStateOf(item?.priority ?: 1) }
    var category by remember { mutableStateOf(item?.category ?: "Umum") }
    var emoji by remember { mutableStateOf(item?.emoji ?: "📝") }
    var date by remember { mutableStateOf(item?.dueDate) }
    var time by remember { mutableStateOf(item?.dueTime) }
    var repeatMode by remember { mutableStateOf(item?.repeatMode ?: "None") }
    var attachmentLink by remember { mutableStateOf(item?.attachmentLink ?: "") }
    
    val context = LocalContext.current
    val categories = listOf("Umum", "Kerja", "Belajar", "Pribadi")
    
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
        containerColor = SpaceDark,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                if (item == null) "MISI BARU" else "PERBARUI MISI", 
                fontWeight = FontWeight.Black, 
                letterSpacing = 2.sp, 
                color = Color.White
            )
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        if (date != null) calendar.timeInMillis = date!!
                        DatePickerDialog(context, { _, y, m, d ->
                            date = Calendar.getInstance().apply { set(y, m, d) }.timeInMillis
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    val sdf = SimpleDateFormat("EEEE, dd/MM", Locale.getDefault())
                    Text(if (date == null) "Tanggal" else sdf.format(Date(date!!)), fontSize = 10.sp)
                }
                
                Button(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        TimePickerDialog(context, { _, h, m ->
                            time = String.format("%02d:%02d", h, m)
                        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(time ?: "Jam", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column {
                Text("KATEGORI", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White.copy(0.4f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        Box(modifier = Modifier.weight(1f)) {
                            CategoryChip(
                                glowColor = glowColor,
                                category = cat,
                                selected = category == cat,
                                onClick = { category = cat }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column {
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column {
                Text("PENGULANGAN", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White.copy(0.4f))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                    listOf("None" to "Tidak Ada", "Daily" to "Harian", "Weekly" to "Mingguan").forEach { (mode, label) ->
                        val active = repeatMode == mode
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) glowColor.copy(0.2f) else Color.Transparent)
                                .border(1.dp, if (active) glowColor else Color.White.copy(0.1f), RoundedCornerShape(8.dp))
                                .clickable { repeatMode = mode }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(label, fontSize = 12.sp, color = if (active) Color.White else Color.White.copy(0.5f))
                        }
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
                    containerColor = glowColor,
                    disabledContainerColor = glowColor.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (item == null) "EKSEKUSI" else "SINKRONISASI", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            }
        }
    }
}
