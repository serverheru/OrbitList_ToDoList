package com.example.orbitlist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo_items")
data class TodoItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val task: String,
    val description: String = "",
    val isDone: Boolean = false,
    val isPinned: Boolean = false,
    val priority: Int = 1, // 0: Low, 1: Medium, 2: High
    val category: String = "Umum",
    val emoji: String = "📝",
    val dueDate: Long? = null,
    val dueTime: String? = null,
    val position: Int = 0,
    val repeatMode: String = "None", // None, Daily, Weekly
    val attachmentLink: String = "",
    val soundUri: String? = null,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
