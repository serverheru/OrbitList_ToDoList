package com.example.school

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val todoDao = TodoDatabase.getDatabase(application).todoDao()
    val allTasks: Flow<List<TodoItem>> = todoDao.getAllTasks()
    private val context = application.applicationContext

    init {
        NotificationHelper.createNotificationChannel(context)
    }

    fun addTask(taskText: String, description: String = "", priority: Int = 1, category: String = "Umum", emoji: String = "📝", dueDate: Long? = null, dueTime: String? = null) {
        viewModelScope.launch {
            val itemToInsert = TodoItem(
                task = taskText,
                description = description,
                priority = priority,
                category = category,
                emoji = emoji,
                dueDate = dueDate,
                dueTime = dueTime
            )
            val newId = todoDao.addTask(itemToInsert)
            // Schedule notification with the real ID
            NotificationHelper.scheduleNotification(context, itemToInsert.copy(id = newId.toInt()))
        }
    }

    fun updateTaskStatus(item: TodoItem, isDone: Boolean) {
        viewModelScope.launch {
            val updatedItem = item.copy(isDone = isDone)
            todoDao.updateTask(updatedItem)
            if (isDone) {
                NotificationHelper.cancelNotification(context, item.id)
            } else {
                NotificationHelper.scheduleNotification(context, updatedItem)
            }
        }
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
