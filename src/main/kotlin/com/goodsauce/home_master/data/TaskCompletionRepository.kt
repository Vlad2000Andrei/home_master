package com.goodsauce.home_master.data

import com.goodsauce.home_master.logic.TaskCompletionCreation
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.entity.*
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.timestamp
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class TaskCompletionRepository(private val database: Database) {

    object TaskCompletions : Table<TaskCompletion>("task_completion") {
        val id = int("id").primaryKey().bindTo { it.id }
        val task = int("task_id").references(TaskRepository.Tasks) { it.task }
        val taskSchedule = int("task_schedule_id").references(TaskScheduleRepository.TaskSchedules) { it.taskSchedule }
        val timeCompleted = timestamp("time_completed").bindTo { it.timeCompleted }
        val timeDue = timestamp("time_due").bindTo { it.timeDue }
    }

    interface TaskCompletion : Entity<TaskCompletion> {
        companion object : Entity.Factory<TaskCompletion>()

        val id: Int
        val task: TaskRepository.Task
        val taskSchedule: TaskScheduleRepository.TaskSchedule
        val timeDue: Instant
        val timeCompleted: Instant?
    }

    fun getByTaskId(taskId: Int, timeRange: ClosedRange<Instant>): List<TaskCompletion> =
        database.sequenceOf(TaskCompletions).filter { it.task.eq(taskId).and(it.timeDue.between(timeRange)) }
            .sortedByDescending { it.timeCompleted }.toList()

    fun getLastByScheduleIds(): Map<Int?, Instant?> {
        val maxTimeDue = max(TaskCompletions.timeDue).aliased("max_due")
        return database.from(TaskCompletions)
            .select(TaskCompletions.taskSchedule, maxTimeDue)
            .groupBy(TaskCompletions.taskSchedule)
            .associate { row ->
                row[TaskCompletions.taskSchedule] to row[maxTimeDue]
            }
    }

    fun insert(completion: TaskCompletionCreation) = database.insert(TaskCompletions) {
        set(it.task, completion.taskId)
        set(it.taskSchedule, completion.taskScheduleId)
        set(it.timeDue, completion.timeDue)
    }

    fun insert(completions: Collection<TaskCompletionCreation>) =
        database.batchInsert(TaskCompletions) {
            completions.forEach { completion ->
                item {
                    set(it.task, completion.taskId)
                    set(it.taskSchedule, completion.taskScheduleId)
                    set(it.timeDue, completion.timeDue)
                }
            }
        }

    fun complete(completionId: Int, completionTime: Instant) {
        database.update(TaskCompletions) {
            where { it.id.eq(completionId) }
            set(it.timeCompleted, completionTime)
        }
    }

    fun getByDueTimestamp(dueTimestampRange : ClosedRange<Instant>) : Set<TaskCompletion> =
        database.sequenceOf(TaskCompletions).filter { it.timeDue.between(dueTimestampRange) }.toSet()


}