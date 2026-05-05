package com.heruvant.orbitlist

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_items ORDER BY isPinned DESC, position ASC, id ASC")
    fun getAllTasks(): Flow<List<TodoItem>>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun addTask(item: TodoItem): Long

    @Update
    suspend fun updateTask(item: TodoItem)

    @Delete
    suspend fun deleteTask(item: TodoItem)

    @Query("DELETE FROM todo_items")
    suspend fun deleteAllTasks()
}