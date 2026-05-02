package com.example.school

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val todoDao = TodoDatabase.getDatabase(application).todoDao()
    val allTasks: Flow<List<TodoItem>> = todoDao.getAllTasks()

    fun addTask(taskText: String, priority: Int = 1, category: String = "Umum", dueDate: Long? = null, dueTime: String? = null) {
        viewModelScope.launch {
            todoDao.addTask(TodoItem(
                task = taskText, 
                priority = priority, 
                category = category,
                dueDate = dueDate,
                dueTime = dueTime
            ))
        }
    }

    fun updateTaskStatus(item: TodoItem, isDone: Boolean) {
        viewModelScope.launch {
            todoDao.updateTask(item.copy(isDone = isDone))
        }
    }

    fun updateTask(item: TodoItem) {
        viewModelScope.launch {
            todoDao.updateTask(item)
        }
    }

    fun deleteTask(item: TodoItem) {
        viewModelScope.launch {
            todoDao.deleteTask(item)
        }
    }

    fun deleteAllTasks() {
        viewModelScope.launch {
            todoDao.deleteAllTasks()
        }
    }
}
