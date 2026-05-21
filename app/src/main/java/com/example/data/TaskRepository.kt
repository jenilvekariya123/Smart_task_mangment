package com.example.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasksFlow()

    suspend fun getTaskById(id: Long): Task? {
        return taskDao.getTaskById(id)
    }

    suspend fun upsert(task: Task): Long {
        return taskDao.upsertTask(task)
    }

    suspend fun delete(task: Task) {
        taskDao.deleteTask(task)
    }

    suspend fun deleteAll() {
        taskDao.deleteAllTasks()
    }
}
