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

            // Reset recurring tasks if next period has started
            val now = System.currentTimeMillis()
            val calendar = Calendar.getInstance()
            val todayMidnight = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            filtered.map { item ->
                if (item.isDone && item.repeatMode != "None" && item.dueDate != null) {
                    // Determine when to reset: Midnight of the day after the dueDate
                    calendar.timeInMillis = item.dueDate
                    if (item.repeatMode == "Daily") {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    } else if (item.repeatMode == "Weekly") {
                        calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    }

                    // Set to start of that next period (Midnight)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)

                    val resetTime = calendar.timeInMillis

                    // Only reset if it wasn't completed today (allows it to stay struck through for the rest of today)
                    val isCompletedToday =
                        item.completedAt != null && item.completedAt!! >= todayMidnight

                    if (now >= resetTime && !isCompletedToday) {
                        // Time to reset for next period
                        val nextDueDate = calculateNextDueDate(item.dueDate, item.repeatMode)
                        val resetItem = item.copy(isDone = false, dueDate = nextDueDate)
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

        // If today is not the same as last update date AND not the day after, streak is broken
        if (today != lastDate && today != dayAfterLast) {
            _streak.value = 0
            prefs.edit().putInt("orbit_streak", 0).apply()
        }
    }

    fun updateStreakIfNeeded(tasks: List<TodoItem>) {
        val total = tasks.size
        val completed = tasks.count { it.isDone }

        if (total > 0 && total == completed) {
            val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val today = sdf.format(Date())
            val lastDate = prefs.getString("last_streak_date", "")

            if (lastDate != today) {
                // Check if it's consecutive
                val current = _streak.value
                val newStreak = if (lastDate.isNullOrEmpty()) 1 else {
                    val lastDateObj = sdf.parse(lastDate)!!
                    val cal = Calendar.getInstance()
                    cal.time = lastDateObj
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    if (sdf.format(cal.time) == today) current + 1 else 1
                }

                _streak.value = newStreak
                prefs.edit()
                    .putInt("orbit_streak", newStreak)
                    .putString("last_streak_date", today)
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
           if (isDone && item.repeatMode != "None") {
                // Stay checked but track time
                val updatedItem = item.copy(
                    isDone = true,
                    completedAt = System.currentTimeMillis()
                )
                todoDao.updateTask(updatedItem)
                NotificationHelper.cancelNotification(context, item.id)
            } else if (item.repeatMode != "None" && !isDone) {
                // If user somehow tries to uncheck a recurring task that's done, ignore it if isDone is already true
                if (item.isDone) return@launch

                val updatedItem = item.copy(
                    isDone = false,
                    completedAt = null
                )
                todoDao.updateTask(updatedItem)
                NotificationHelper.scheduleNotification(context, updatedItem)
            } else {
                // Regular one-time task logic
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
    }

    private fun calculateNextDueDate(currentDate: Long, repeatMode: String): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentDate
        val now = System.currentTimeMillis()

        // Ensure the next due date is in the future
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
            val newList = currentList.toMutableList()
            if (fromIndex in newList.indices && toIndex in newList.indices) {
                val item = newList.removeAt(fromIndex)
                newList.add(toIndex, item)

                // Update all positions in database
                newList.forEachIndexed { index, todoItem ->
                    if (todoItem.position != index) {
                        todoDao.updateTask(todoItem.copy(position = index))
                    }
                }
            }
        }
    }

    fun deleteAllTasks() {
        viewModelScope.launch {
            todoDao.deleteAllTasks()
        }
    }
}