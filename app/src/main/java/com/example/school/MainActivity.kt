package com.example.school

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.school.ui.theme.*
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TodoApp(
    viewModel: TodoViewModel = viewModel()
) {
    var editingItem by remember { mutableStateOf<TodoItem?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var filterCategory by remember { mutableStateOf("Semua") }
    
    val todoList by viewModel.allTasks.collectAsState(initial = emptyList())
    val categories = listOf("Umum", "Kerja", "Belajar", "Pribadi", "Belanja")
    val filterCategories = listOf("Semua") + categories
    val filteredList = todoList.filter { filterCategory == "Semua" || it.category == filterCategory }

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
                Brush.radialGradient(listOf(ElectricIndigo.copy(0.15f), Color.Transparent)),
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
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "TUGAS KUANTUM",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            floatingActionButton = {
                InteractiveFAB(onClick = { 
                    editingItem = null
                    showDialog = true 
                })
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                
                GlassCard(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(
                                    color = Color.White.copy(0.05f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 12f, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    brush = Brush.sweepGradient(listOf(ElectricIndigo, NeonCyan, ElectricIndigo)),
                                    startAngle = -90f,
                                    sweepAngle = progressAnim * 360f,
                                    useCenter = false,
                                    style = Stroke(width = 12f, cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${(progressAnim * 100).toInt()}%", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color.White)
                                Text("SELESAI", fontSize = 10.sp, letterSpacing = 2.sp, color = NeonCyan)
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(24.dp))
                        
                        Column {
                            Text(filterCategory.uppercase(), fontWeight = FontWeight.Bold, color = NeonCyan, letterSpacing = 1.sp)
                            Text("$completedTasks / $totalTasks Tugas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Produktivitas meningkat!", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.6f))
                        }
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filterCategories) { cat ->
                        CategoryChip(
                            category = cat,
                            selected = filterCategory == cat,
                            onClick = { filterCategory = cat }
                        )
                    }
                }

                if (filteredList.isEmpty()) {
                    CyberEmptyState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredList, key = { it.id }) { item ->
                            AnimatedVisibility(
                                visible = true,
                                enter = slideInVertically() + fadeIn(),
                                modifier = Modifier.animateItem()
                            ) {
                                CyberTodoRow(
                                    item = item,
                                    onToggle = { viewModel.updateTaskStatus(item, !item.isDone) },
                                    onDelete = { viewModel.deleteTask(item) },
                                    onEdit = {
                                        editingItem = item
                                        showDialog = true
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
        CyberAddEditDialog(
            item = editingItem,
            onDismiss = { showDialog = false },
            onSave = { text, p, c, d, t ->
                if (editingItem == null) {
                    viewModel.addTask(text, p, c, d, t)
                } else {
                    viewModel.updateTask(editingItem!!.copy(
                        task = text,
                        priority = p,
                        category = c,
                        dueDate = d,
                        dueTime = t
                    ))
                }
                showDialog = false
            }
        )
    }
}

@Composable
fun CategoryChip(category: String, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1.1f else 1f)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) ElectricIndigo else GlassSurface,
        modifier = Modifier.scale(scale).shadow(if (selected) 12.dp else 0.dp, RoundedCornerShape(16.dp))
    ) {
        Text(
            category, 
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else Color.White.copy(0.7f)
        )
    }
}

@Composable
fun InteractiveFAB(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.85f else 1f)
    
    Box(
        modifier = Modifier
            .scale(scale)
            .size(72.dp)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .background(Brush.linearGradient(listOf(ElectricIndigo, VividViolet)), CircleShape)
            .shadow(24.dp, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
    }
}

@Composable
fun CyberTodoRow(item: TodoItem, onToggle: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) {
    val pColor = when(item.priority) {
        2 -> CyberRed
        1 -> CyberAmber
        else -> CyberGreen
    }
    
    val alpha by animateFloatAsState(if (item.isDone) 0.4f else 1f)
    
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(24.dp),
        color = if (item.isDone) Color.White.copy(0.02f) else GlassSurface,
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).alpha(alpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (item.isDone) pColor else Color.White.copy(0.2f), CircleShape)
                    .background(if (item.isDone) pColor else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (item.isDone) Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp), tint = SpaceDark)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.task, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 18.sp, 
                    color = Color.White,
                    style = if (item.isDone) MaterialTheme.typography.bodyLarge.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else MaterialTheme.typography.bodyLarge
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.category.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black, color = pColor)
                    
                    if (item.dueDate != null || item.dueTime != null) {
                        Text(" • ", color = Color.White.copy(0.3f))
                        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(12.dp), tint = Color.White.copy(0.5f))
                        Text(
                            text = buildString {
                                if (item.dueDate != null) append(SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(item.dueDate)))
                                if (item.dueTime != null) {
                                    if (isNotEmpty()) append(" ")
                                    append(item.dueTime)
                                }
                            },
                            fontSize = 10.sp, 
                            color = Color.White.copy(0.5f)
                        )
                    }
                }
            }
            
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(20.dp))
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White.copy(0.05f))
            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(32.dp))
    ) {
        content()
    }
}

@Composable
fun CyberEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(150.dp)) {
                drawCircle(ElectricIndigo.copy(0.1f))
            }
            Icon(Icons.Default.RocketLaunch, null, modifier = Modifier.size(60.dp), tint = ElectricIndigo)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("STASI BERSIH", fontWeight = FontWeight.Black, letterSpacing = 4.sp, color = Color.White)
        Text("Menunggu misi selanjutnya...", color = Color.White.copy(0.5f), fontSize = 12.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberAddEditDialog(
    item: TodoItem?,
    onDismiss: () -> Unit, 
    onSave: (String, Int, String, Long?, String?) -> Unit
) {
    var text by remember { mutableStateOf(item?.task ?: "") }
    var priority by remember { mutableIntStateOf(item?.priority ?: 1) }
    var category by remember { mutableStateOf(item?.category ?: "Umum") }
    var date by remember { mutableStateOf(item?.dueDate) }
    var time by remember { mutableStateOf(item?.dueTime) }
    
    val context = LocalContext.current
    val categories = listOf("Umum", "Kerja", "Belajar", "Pribadi", "Belanja")

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.9f).clip(RoundedCornerShape(32.dp)).background(SpaceDark).border(1.dp, ElectricIndigo.copy(0.3f), RoundedCornerShape(32.dp)),
        content = {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    if (item == null) "MISI BARU" else "PERBARUI MISI", 
                    fontWeight = FontWeight.Black, 
                    letterSpacing = 2.sp, 
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Tujuan misi...", color = Color.White.copy(0.3f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(0.05f),
                        unfocusedContainerColor = Color.White.copy(0.05f),
                        focusedTextColor = Color.White,
                        cursorColor = NeonCyan,
                        focusedIndicatorColor = NeonCyan
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
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
                        Text(if (date == null) "Tanggal" else SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(date!!)), fontSize = 12.sp)
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
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                        categories.forEach { cat ->
                            val active = category == cat
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) ElectricIndigo.copy(0.2f) else Color.Transparent)
                                    .border(1.dp, if (active) ElectricIndigo else Color.White.copy(0.1f), RoundedCornerShape(8.dp))
                                    .clickable { category = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(cat, fontSize = 12.sp, color = if (active) Color.White else Color.White.copy(0.5f))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("RENDAH" to 0, "SEDANG" to 1, "TINGGI" to 2).forEach { (label, value) ->
                        val active = priority == value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) ElectricIndigo else Color.White.copy(0.05f))
                                .clickable { priority = value },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontWeight = FontWeight.Bold, color = if (active) Color.White else Color.White.copy(0.5f), fontSize = 12.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { if(text.isNotBlank()) onSave(text, priority, category, date, time) },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (item == null) "EKSEKUSI" else "SINKRONISASI", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                }
            }
        }
    )
}
