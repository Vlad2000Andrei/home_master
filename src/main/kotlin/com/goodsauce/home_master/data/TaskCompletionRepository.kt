package com.goodsauce.home_master.data

import org.ktorm.database.Database
import org.ktorm.dsl.insert
import org.ktorm.entity.Entity
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.timestamp
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class TaskCompletionRepository(private val database: Database) {

    object TaskCompletions : Table<TaskCompletion>("task_completion") {
        val id = int("id").primaryKey().bindTo { it.id }
        val taskId = int("task_id").bindTo { it.taskId }
        val timestamp = timestamp("time_completed").bindTo { it.timestamp }
    }

    interface TaskCompletion : Entity<TaskCompletion> {
        companion object : Entity.Factory<TaskCompletion>()

        val id: Int
        val taskId: Int
        val timestamp: Instant
    }

    fun complete(taskId: Int, completionTime : Instant) {
        database.insert(TaskCompletions) {
            set(it.taskId, taskId)
            set(it.timestamp, completionTime)
        }
    }

}