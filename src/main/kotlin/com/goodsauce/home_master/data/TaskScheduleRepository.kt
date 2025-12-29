package com.goodsauce.home_master.data

import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.entity.Entity
import org.ktorm.entity.filter
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.toSet
import org.ktorm.schema.*
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalTime

@Repository
class TaskScheduleRepository(private val database: Database) {

    object TaskSchedules : Table<TaskSchedule>("task_schedule") {
        val id = int("id").primaryKey().bindTo { it.id }
        val taskId = int("task_id").references(TaskRepository.Tasks) { it.task }
        val repeatRate = enum<TaskScheduleRepeatRate>("repeat_rate").bindTo { it.repeatRate }
        val scheduledTime = time("scheduled_time").bindTo { it.scheduledTime }
        val startDate = date("start_date").bindTo { it.startDate }
        val name = text("name").bindTo { it.name }
    }

    interface TaskSchedule : Entity<TaskSchedule> {
        companion object : Entity.Factory<TaskSchedule>()

        val id: Int
        val task: TaskRepository.Task
        val repeatRate: TaskScheduleRepeatRate
        val scheduledTime: LocalTime
        val startDate: LocalDate
        val name: String
    }

    fun getByTaskId(taskId: Int): Set<TaskSchedule> =
        database.sequenceOf(TaskSchedules).filter { it.taskId.eq(taskId) }.toSet()

    fun getAll(): Set<TaskSchedule> = database.sequenceOf(TaskSchedules).toSet()
}
