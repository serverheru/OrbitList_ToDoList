package com.heruvant.orbitlist

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val todoDao = TodoDatabase.getDatabase(application).todoDao()
    private val context = application.applicationContext

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: Flow<String> = _searchQuery

    private val rawTasks: Flow<List<TodoItem>> = todoDao.getAllTasks()

    private val prefs = application.getSharedPreferences("OrbitListPrefs", Context.MODE_PRIVATE)
    private val _streak = MutableStateFlow(prefs.getInt("orbit_streak", 0))
    val streak: StateFlow<Int> = _streak.asStateFlow()

    private val _categories = MutableStateFlow(
        prefs.getStringSet("custom_categories", setOf("Umum", "Kerja", "Belajar", "Pribadi"))
            ?.toList() ?: listOf("Umum", "Kerja", "Belajar", "Pribadi")
    )
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    val allTasks: Flow<List<TodoItem>> =
        combine(todoDao.getAllTasks(), _searchQuery) { tasks, query ->
            val filtered = if (query.isBlank()) {
                tasks
            } else {
                tasks.filter {
                    it.task.contains(query, ignoreCase = true) ||
                            it.description.contains(query, ignoreCase = true)
                }
            }

            val now = System.currentTimeMillis()
            val calendar = Calendar.getInstance()
            
            // Fixed 00:00 Reset Logic
            filtered.map { item ->
                if (item.repeatMode != "None" && item.dueDate != null) {
                    calendar.timeInMillis = item.dueDate
                    
                    // Reset point is 00:00 of the day AFTER the due date
                    val resetCalendar = Calendar.getInstance().apply {
                        timeInMillis = item.dueDate
                        if (item.repeatMode == "Daily") {
                            add(Calendar.DAY_OF_YEAR, 1)
                        } else if (item.repeatMode == "Weekly") {
                            add(Calendar.WEEK_OF_YEAR, 1)
                        }
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    if (now >= resetCalendar.timeInMillis) {
                        // Time to reset
                        val nextDueDate = calculateNextDueDate(item.dueDate, item.repeatMode)
                        val resetItem = item.copy(
                            isDone = false, 
                            dueDate = nextDueDate, 
                            completedAt = null // Ensure indicator goes to 0
                        )
                        viewModelScope.launch { todoDao.updateTask(resetItem) }
                        resetItem
                    } else {
                        item
                    }
                } else {
                    item
                }
            }
        }

    init {
        NotificationHelper.createNotificationChannel(context)
        checkAndResetStreak()
        
        // Auto-Pilot and Audit on Launch
        viewModelScope.launch {
            delay(1000)
            checkAndAutoExecuteTasks()
        }

        // Auto update streak when tasks change
        viewModelScope.launch {
            rawTasks.collect { tasks ->
                updateStreakIfNeeded(tasks)
            }
        }
    }

    private fun checkAndResetStreak() {
        val lastDate = prefs.getString("last_streak_date", "")
        if (lastDate.isNullOrEmpty()) return

        val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val today = sdf.format(Date())
        val lastDateObj = sdf.parse(lastDate) ?: return

        val calendar = Calendar.getInstance()
        calendar.time = lastDateObj
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val dayAfterLast = sdf.format(calendar.time)

        if (today != lastDate && today != dayAfterLast) {
            _streak.value = 0
            prefs.edit().putInt("orbit_streak", 0).apply()
        }
    }

    private fun checkAndAutoExecuteTasks() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val tasks = todoDao.getAllTasks().first()
            
            tasks.forEach { item ->
                if (!item.isDone && item.dueDate != null && item.dueTime != null) {
                    val target = Calendar.getInstance().apply {
                        timeInMillis = item.dueDate
                        val parts = item.dueTime.split(":")
                        set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                        set(Calendar.MINUTE, parts[1].toInt())
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    
                    val threshold = 10 * 60 * 1000 // 10 minutes
                    if (now > target.timeInMillis + threshold) {
                        val updatedItem = item.copy(
                            isDone = true,
                            completedAt = now,
                            description = if (item.description.contains("🤖 AUTO-PILOT")) item.description 
                                         else item.description + "\n\n🤖 AUTO-PILOT: Dieksekusi otomatis (User tidak respons > 10m)"
                        )
                        todoDao.updateTask(updatedItem)
                        NotificationHelper.cancelNotification(context, item.id)
                    }
                }
            }
        }
    }

    fun updateStreakIfNeeded(tasks: List<TodoItem>) {
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val tomorrowCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }

        // Only count tasks due today or before (overdue)
        val relevantTasks = tasks.filter { 
            it.dueDate != null && it.dueDate!! < tomorrowCal.timeInMillis 
        }

        if (relevantTasks.isEmpty()) return

        val total = relevantTasks.size
        val completed = relevantTasks.count { it.isDone }

        if (total > 0 && total == completed) {
            val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val todayStr = sdf.format(Date())
            val lastDate = prefs.getString("last_streak_date", "")

            if (lastDate != todayStr) {
                val current = _streak.value
                val newStreak = if (lastDate.isNullOrEmpty()) 1 else {
                    val lastDateObj = sdf.parse(lastDate)!!
                    val cal = Calendar.getInstance()
                    cal.time = lastDateObj
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    if (sdf.format(cal.time) == todayStr) current + 1 else 1
                }

                _streak.value = newStreak
                prefs.edit()
                    .putInt("orbit_streak", newStreak)
                    .putString("last_streak_date", todayStr)
                    .apply()
            }
        }
    }

    fun addCategory(category: String) {
        val current = _categories.value.toMutableList()
        if (!current.contains(category)) {
            current.add(category)
            _categories.value = current
            prefs.edit().putStringSet("custom_categories", current.toSet()).apply()
        }
    }

    fun removeCategory(category: String) {
        val current = _categories.value.toMutableList()
        if (current.size > 1 && current.contains(category)) {
            current.remove(category)
            _categories.value = current
            prefs.edit().putStringSet("custom_categories", current.toSet()).apply()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addTask(
        taskText: String,
        description: String = "",
        priority: Int = 1,
        category: String = "Umum",
        emoji: String = "📝",
        dueDate: Long? = null,
        dueTime: String? = null,
        repeatMode: String = "None",
        attachmentLink: String = "",
        soundUri: String? = null
    ) {
        viewModelScope.launch {
            val itemToInsert = TodoItem(
                task = taskText,
                description = description,
                priority = priority,
                category = category,
                emoji = emoji,
                dueDate = dueDate,
                dueTime = dueTime,
                repeatMode = repeatMode,
                attachmentLink = attachmentLink,
                soundUri = soundUri
            )
            val newId = todoDao.addTask(itemToInsert)
            NotificationHelper.scheduleNotification(context, itemToInsert.copy(id = newId.toInt()))
        }
    }

    fun updateTaskStatus(item: TodoItem, isDone: Boolean) {
        viewModelScope.launch {
            val updatedItem = item.copy(
                isDone = isDone,
                completedAt = if (isDone) System.currentTimeMillis() else null
            )
            todoDao.updateTask(updatedItem)

            if (isDone) {
                NotificationHelper.cancelNotification(context, item.id)
            } else {
                NotificationHelper.scheduleNotification(context, updatedItem)
            }
        }
    }

    private fun calculateNextDueDate(currentDate: Long, repeatMode: String): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentDate
        val now = System.currentTimeMillis()

        while (calendar.timeInMillis <= now) {
            when (repeatMode) {
                "Daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                else -> break
            }
        }
        return calendar.timeInMillis
    }

    fun togglePin(item: TodoItem) {
        viewModelScope.launch {
            todoDao.updateTask(item.copy(isPinned = !item.isPinned))
        }
    }

    fun updateTask(item: TodoItem) {
        viewModelScope.launch {
            todoDao.updateTask(item)
            NotificationHelper.scheduleNotification(context, item)
        }
    }

    fun deleteTask(item: TodoItem) {
        viewModelScope.launch {
            todoDao.deleteTask(item)
            NotificationHelper.cancelNotification(context, item.id)
        }
    }

    fun moveTask(fromIndex: Int, toIndex: Int, currentList: List<TodoItem>) {
        viewModelScope.launch {
            if (fromIndex in currentList.indices && toIndex in currentList.indices) {
                val fromItem = currentList[fromIndex]
                val toItem = currentList[toIndex]
                
                val fromPos = fromItem.position
                val toPos = toItem.position
                
                // Swap positions specifically
                todoDao.updateTask(fromItem.copy(position = toPos))
                todoDao.updateTask(toItem.copy(position = fromPos))
            }
        }
    }

    fun deleteAllTasks() {
        viewModelScope.launch {
            todoDao.deleteAllTasks()
        }
    }
}
