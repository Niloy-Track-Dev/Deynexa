package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.repository.TaskRepositoryImpl
import com.example.domain.repository.TaskRepository
import com.example.domain.service.BackupService
import com.example.domain.service.SchedulingService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DaynexaApplication : Application() {
    lateinit var database: AppDatabase
    lateinit var repository: TaskRepository
    lateinit var schedulingService: SchedulingService
    lateinit var backupService: BackupService

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "daynexa.db"
        ).build()

        repository = TaskRepositoryImpl(
            database.categoryDao(),
            database.taskDao(),
            database.taskOccurrenceDao(),
            database.settingDao()
        )
        
        schedulingService = SchedulingService()
        backupService = BackupService()
        
        initDemoData()
    }

    private fun initDemoData() {
        kotlinx.coroutines.MainScope().launch {
            val categories = repository.getCategories().first()
            if (categories.isEmpty()) {
                val healthId = repository.saveCategory(com.example.domain.model.Category(name = "Health", icon = "favorite", color = 0xFFE91E63.toInt()))
                val workId = repository.saveCategory(com.example.domain.model.Category(name = "Work", icon = "work", color = 0xFF2196F3.toInt()))
                val personalId = repository.saveCategory(com.example.domain.model.Category(name = "Personal", icon = "person", color = 0xFF4CAF50.toInt()))

                repository.saveTask(com.example.domain.model.Task(
                    name = "Wake Up",
                    categoryId = personalId,
                    startTime = 420, // 07:00
                    endTime = 420,
                    isRecurring = true,
                    recurringDays = java.time.DayOfWeek.values().toSet()
                ))
                repository.saveTask(com.example.domain.model.Task(
                    name = "Exercise",
                    categoryId = healthId,
                    startTime = 420, // 07:00
                    endTime = 465, // 07:45
                    isRecurring = true,
                    recurringDays = setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.FRIDAY, java.time.DayOfWeek.SATURDAY)
                ))
            }
        }
    }
}
