package com.goodsauce.home_master.logic

import com.goodsauce.home_master.data.TaskCompletionRepository
import com.goodsauce.home_master.data.TaskRepository
import com.goodsauce.home_master.data.TaskScheduleRepeatRate
import com.goodsauce.home_master.data.TaskScheduleRepository
import com.goodsauce.home_master.support.LOG
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration.Companion.days

@Service
class TaskService(
    private val taskRepository: TaskRepository,
    private val taskCompletionRepository: TaskCompletionRepository,
    private val taskScheduleRepository: TaskScheduleRepository,
    @Value("\${tasks.lookback-days}") private val taskLookbackDays: Int,
    @Value("\${tasks.time-offset-tolerance}") private val completionTimeOffsetTolerance: Double,
    @Value("\${tasks.generate-ahead-time}") private val generateAheadTime: java.time.Duration,
) {
    private val zoneIdAmsterdam: ZoneId = ZoneId.of("Europe/Amsterdam")
    private val clock = Clock.system(zoneIdAmsterdam)
    private val taskLookback = taskLookbackDays.days

    @Scheduled(fixedRateString = "\${tasks.generation-repeat-period}")
    fun generateTaskCompletions() {
        val now = Instant.now(clock)
        val latestDueDateToGenerate = now.plus(generateAheadTime)
        val generationTimeRange = Instant.MIN..latestDueDateToGenerate
        val latestCompletionsBySchedule = taskCompletionRepository.getLastByScheduleIds()
        val taskSchedules = taskScheduleRepository.getAll()
        val latestCompletionDueTimeBySchedule = taskSchedules.associateWith { latestCompletionsBySchedule[it.id] }
        val nextCompletionDueTimeBySchedule = latestCompletionDueTimeBySchedule.entries.associate {
            Pair(
                it.key,
                it.value?.plusMillis(it.key.repeatRate.completionInterval.inWholeMilliseconds)
                    ?: getFirstExpectedCompletion(toTaskSchedule(it.key))
            )
        }
        val completionsToInsert = nextCompletionDueTimeBySchedule.map {
            TaskCompletionCreation(
                it.key.task.id, it.key.id, it.value
            )
        }.filter { generationTimeRange.contains(it.timeDue) }.toSet()

        if (completionsToInsert.isNotEmpty()) {
            taskCompletionRepository.insert(completionsToInsert)
        }
        LOG.info { "Generated tasks: $completionsToInsert" }
    }

    fun complete(completionId: Int) {
        val completionTime = Instant.now(clock)
        taskCompletionRepository.complete(completionId, completionTime)

        LOG.info { "Completed task with id $completionId at $completionTime" }
    }

    fun getRelevantCompletions(taskId: Int): TaskSummary {
        val now = Instant.now(clock)
        val task = toTask(taskRepository.getById(taskId))
        val timeMargin = taskLookback * (1 + completionTimeOffsetTolerance)
        val timeRange =
            (now.minusMillis(timeMargin.inWholeMilliseconds)..now.plusMillis(timeMargin.inWholeMilliseconds))

        val relevantCompletions =
            taskCompletionRepository.getByTaskId(taskId, timeRange).sortedBy { it.timeDue }.map { toTaskCompletion(it) }
        val completedCompletions = relevantCompletions.filter { it.timeCompleted != null }
        val outstandingCompletions = relevantCompletions.filter { it.timeCompleted == null }
        return TaskSummary(task, completedCompletions, outstandingCompletions)
    }

    private fun getSchedules(taskId: Int): Set<TaskSchedule> = taskScheduleRepository.getByTaskId(taskId)
        .map { TaskSchedule(it.task.id, it.repeatRate, it.scheduledTime, it.startDate, it.name) }.toSet()

    private fun calculateMaxExpectedCompletionCount(schedules: Set<TaskSchedule>) = schedules.asSequence().map {
        when (it.repeatRate) {
            TaskScheduleRepeatRate.DAILY -> 7
            TaskScheduleRepeatRate.WEEKLY -> 1
        }
    }.sum()

    private fun getFirstExpectedCompletion(schedule: TaskSchedule): Instant = Instant.from(
        schedule.startDate.atTime(schedule.scheduledTime).atZone(zoneIdAmsterdam)
    )

    private fun getPreviousExpectedCompletionTime(schedule: TaskSchedule): Instant {
        val firstExpectedCompletion = getFirstExpectedCompletion(schedule)
        val nextExpectedCompletion = getNextExpectedCompletionTime(schedule)
        val previousExpectedCompletion =
            nextExpectedCompletion.minusMillis(schedule.repeatRate.completionInterval.inWholeMilliseconds)
        return if (previousExpectedCompletion.isBefore(firstExpectedCompletion)) firstExpectedCompletion else previousExpectedCompletion
    }

    private fun getNextExpectedCompletionTime(schedule: TaskSchedule): Instant {
        val now = Instant.now()
        val firstExpectedCompletion = getFirstExpectedCompletion(schedule)

        if (now.isBefore(firstExpectedCompletion)) {
            return firstExpectedCompletion
        }

        val timeSinceFirstExpectedCompletion = java.time.Duration.between(firstExpectedCompletion, now).toMillis()
        val durationBetweenCompletions = schedule.repeatRate.completionInterval.inWholeMilliseconds
        return now.plusMillis(timeSinceFirstExpectedCompletion % durationBetweenCompletions)
    }

    private fun toTaskSchedule(schedule: TaskScheduleRepository.TaskSchedule): TaskSchedule = TaskSchedule(
        schedule.id, schedule.repeatRate, schedule.scheduledTime, schedule.startDate, schedule.name
    )

    private fun toTaskCompletion(completion: TaskCompletionRepository.TaskCompletion): TaskCompletion = TaskCompletion(
        completion.id,
        completion.task.id,
        completion.taskSchedule.id,
        completion.taskSchedule.name,
        completion.timeCompleted,
        completion.timeDue
    )

    private fun toTask(task: TaskRepository.Task): Task = Task(task.id, task.name)
}