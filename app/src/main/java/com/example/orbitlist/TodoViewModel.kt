package com.example.orbitlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val todoDao = TodoDatabase.getDatabase(application).todoDao()
    private val context = application.applicationContext

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: Flow<String> = _searchQuery

    val allTasks: Flow<List<TodoItem>> = combine(todoDao.getAllTasks(), _searchQuery) { tasks, query ->
        if (query.isBlank()) {
            tasks
        } else {
            tasks.filter { 
                it.task.contains(query, ignoreCase = true) || 
                it.description.contains(query, ignoreCase = true) 
            }
        }
    }

    init {
        NotificationHelper.createNotificationChannel(context)
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

    private suspend fun handleRecurringTask(item: TodoItem) {
        val nextDueDate = calculateNextDueDate(item.dueDate ?: System.currentTimeMillis(), item.repeatMode)
        val newItem = item.copy(
            id = 0,
            isDone = false,
            dueDate = nextDueDate,
            completedAt = null,
            createdAt = System.currentTimeMillis()
        )
        val newId = todoDao.addTask(newItem)
        NotificationHelper.scheduleNotification(context, newItem.copy(id = newId.toInt()))
    }

    private fun calculateNextDueDate(currentDate: Long, repeatMode: String): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentDate
        when (repeatMode) {
            "Daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
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
