package com.example.school

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.school.ui.theme.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SchoolTheme {
                TodoApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoApp(
    viewModel: TodoViewModel = viewModel()
) {
    var taskText by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableIntStateOf(1) }
    var selectedCategory by remember { mutableStateOf("Umum") }
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var selectedTime by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var filterCategory by remember { mutableStateOf("Semua") }
    
    val context = LocalContext.current
    val todoList by viewModel.allTasks.collectAsState(initial = emptyList())
    
    val categories = listOf("Umum", "Kerja", "Belajar", "Pribadi", "Belanja")
    val filterCategories = listOf("Semua") + categories

    val filteredList = remember(todoList, filterCategory) {
        if (filterCategory == "Semua") todoList else todoList.filter { it.category == filterCategory }
    }

    val totalTasks = filteredList.size
    val completedTasks = filteredList.count { it.isDone }
    val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f

    val addTask = {
        if (taskText.isNotBlank()) {
            viewModel.addTask(taskText, selectedPriority, selectedCategory, selectedDate, selectedTime)
            taskText = ""
            selectedPriority = 1
            selectedCategory = "Umum"
            selectedDate = null
            selectedTime = null
            showDialog = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Task Master",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.headlineMedium,
                        letterSpacing = 1.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    if (todoList.isNotEmpty()) {
                        IconButton(onClick = { viewModel.deleteAllTasks() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task", modifier = Modifier.size(32.dp))
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    )
                )
            )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Category Filter Scrollable Row
                ScrollableTabRow(
                    selectedTabIndex = filterCategories.indexOf(filterCategory),
                    edgePadding = 16.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = {}
                ) {
                    filterCategories.forEach { category ->
                        val selected = filterCategory == category
                        FilterChip(
                            selected = selected,
                            onClick = { filterCategory = category },
                            label = { Text(category) },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // Progress Header
                    if (totalTasks > 0 || todoList.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                                .shadow(8.dp, RoundedCornerShape(32.dp)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(32.dp)
                        ) {
                            Box(modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                        )
                                    )
                                )
                                .padding(24.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            if (filterCategory == "Semua") "Ringkasan Tugas" else "Progres $filterCategory",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "$completedTasks dari $totalTasks tugas selesai",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(12.dp)
                                                .clip(RoundedCornerShape(6.dp)),
                                            strokeCap = StrokeCap.Round
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(20.dp))
                                    Box(contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.size(60.dp),
                                            strokeWidth = 6.dp,
                                            strokeCap = StrokeCap.Round
                                        )
                                        Text(
                                            "${(progress * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (filteredList.isEmpty()) {
                        EmptyState(Modifier.weight(1f))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredList, key = { it.id }) { item ->
                                TodoRow(
                                    item = item,
                                    onCheckedChange = { isChecked ->
                                        viewModel.updateTask(item, isDone = isChecked)
                                    },
                                    onDelete = {
                                        viewModel.deleteTask(item)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { 
                Text(
                    "Buat Tugas Baru", 
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                ) 
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    OutlinedTextField(
                        value = taskText,
                        onValueChange = { taskText = it },
                        placeholder = { Text("Apa rencana hebatmu hari ini?") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                    
                    Column {
                        Text("Pilih Kategori", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat) },
                                    modifier = Modifier.padding(end = 8.dp),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                val calendar = Calendar.getInstance()
                                DatePickerDialog(context, { _, y, m, d ->
                                    val picked = Calendar.getInstance().apply { set(y, m, d) }
                                    selectedDate = picked.timeInMillis
                                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selectedDate == null) "Tanggal" else SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(selectedDate!!)))
                        }
                        
                        Button(
                            onClick = {
                                val calendar = Calendar.getInstance()
                                TimePickerDialog(context, { _, h, m ->
                                    selectedTime = String.format("%02d:%02d", h, m)
                                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(selectedTime ?: "Jam")
                        }
                    }

                    Column {
                        Text("Prioritas", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Rendah" to 0, "Sedang" to 1, "Tinggi" to 2).forEach { (label, value) ->
                                val pColor = when(value) {
                                    2 -> PriorityHigh
                                    1 -> PriorityMedium
                                    else -> PriorityLow
                                }
                                FilterChip(
                                    selected = selectedPriority == value,
                                    onClick = { selectedPriority = value },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = pColor,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = addTask,
                    enabled = taskText.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Simpan Tugas", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(32.dp)
        )
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.RocketLaunch,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Langit adalah batasnya!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Tambah tugas untuk memulai petualanganmu.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TodoRow(
    item: TodoItem,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val animatedColor by animateColorAsState(
        if (item.isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surface,
        label = "color"
    )

    val priorityColor = when(item.priority) {
        2 -> PriorityHigh
        1 -> PriorityMedium
        else -> PriorityLow
    }

    Card(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = animatedColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onCheckedChange(!item.isDone) },
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (item.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = if (item.isDone) Icons.Default.Check else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (item.isDone) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = item.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = priorityColor,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .background(priorityColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                    
                    if (item.dueDate != null || item.dueTime != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(0.6f)) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = buildString {
                                    if (item.dueDate != null) append(SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(item.dueDate)))
                                    if (item.dueTime != null) {
                                        if (isNotEmpty()) append(" • ")
                                        append(item.dueTime)
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = item.task,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (item.isDone) FontWeight.Normal else FontWeight.Bold,
                        textDecoration = if (item.isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                    ),
                    color = if (item.isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) 
                            else MaterialTheme.colorScheme.onSurface
                )
            }
            
            IconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus")
            }
        }
    }
}
