package com.goodsauce.home_master.data

import org.ktorm.entity.Entity
import org.ktorm.schema.*
import org.springframework.stereotype.Repository
import java.time.LocalTime

@Repository
class TaskScheduleRepository {

    object TaskSchedules : Table<TaskSchedule>("task_schedule") {
        val id = int("id").primaryKey().bindTo { it.id }
        val taskId = int("task_id").bindTo { it.taskId }
        val repeatRate = enum<TaskScheduleRepeatRate>("repeat_rate").bindTo { it.repeatRate }
        val scheduledTime = time("scheduled_time").bindTo { it.scheduledTime }
    }

    interface TaskSchedule : Entity<TaskSchedule> {
        companion object : Entity.Factory<TaskSchedule>()

        val id : Int
        val taskId : Int
        val repeatRate : TaskScheduleRepeatRate
        val scheduledTime : LocalTime
    }

}