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
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.days

@Service
class TaskService(
    private val taskRepository: TaskRepository,
    private val taskCompletionRepository: TaskCompletionRepository,
    private val taskScheduleRepository: TaskScheduleRepository,
    private val notificationService: NotificationService,
    @Value("\${tasks.lookback-days}") private val taskLookbackDays: Int,
    @Value("\${tasks.time-offset-tolerance}") private val completionTimeOffsetTolerance: Double,
    @Value("\${tasks.generate-ahead-time}") private val generateAheadTime: java.time.Duration,
    @Value("\${tasks.notification-check-period}") private val notificationCheckPeriod: java.time.Duration,
) {
    private val zoneIdAmsterdam: ZoneId = ZoneId.of("Europe/Amsterdam")
    private val clock = Clock.system(zoneIdAmsterdam)
    private val taskLookback = taskLookbackDays.days

    @Scheduled(fixedRateString = "\${tasks.generation-repeat-period}")
    fun generateTaskCompletions(): Set<TaskCompletionCreation> {
        val now = Instant.now(clock)
        val latestDueDateToGenerate = now.plus(generateAheadTime)
        val generationTimeRange = Instant.MIN..latestDueDateToGenerate
        val latestCompletionsBySchedule = taskCompletionRepository.getLastByScheduleIds()
        val taskSchedules = taskScheduleRepository.getAll()
        val latestCompletionDueTimeBySchedule = taskSchedules.associateWith { latestCompletionsBySchedule[it.id] }
        val nextCompletionDueTimeBySchedule = latestCompletionDueTimeBySchedule.entries.associate {
            Pair(
                it.key,
                getCompletionInstantsUntil(toTaskSchedule(it.key), it.value, latestDueDateToGenerate)
            )
        }
        val completionsToInsert = nextCompletionDueTimeBySchedule.flatMap {
            val schedule = it.key
            it.value.map {
                TaskCompletionCreation(schedule.task.id, schedule.id, it)
            }
        }.filter { generationTimeRange.contains(it.timeDue) }.sortedBy { it.timeDue }.toSet()

        if (completionsToInsert.isNotEmpty()) {
            taskCompletionRepository.insert(completionsToInsert)
        }
        LOG.info { "Generated tasks: $completionsToInsert" }
        return completionsToInsert
    }

    @Scheduled(fixedRateString = "\${tasks.notification-check-period}")
    fun checkNotifications() {
        val now = clock.instant()
        val earliestDueDateToFetch = now.minus(notificationCheckPeriod)
        val completions = taskCompletionRepository.getByDueTimestamp(earliestDueDateToFetch .. now)
            .filter { it.timeCompleted == null }

        completions.forEach {
            val subtopic = it.task.name.lowercase().replace("\\s".toRegex(), "-")
            val message = "${it.task.name} (${it.taskSchedule.name}) @ ${it.timeDue.atZone(zoneIdAmsterdam).format(
                DateTimeFormatter.ofPattern("HH:mm"))}"
            notificationService.sendNotification(message, subtopic)
        }
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

    private fun getCompletionInstantsUntil(
        schedule: TaskSchedule,
        latestCompletionDueTime: Instant?,
        until: Instant
    ): Sequence<Instant> {
        val repeatIntervalMillis = schedule.repeatRate.completionInterval.inWholeMilliseconds
        val firstScheduledCompletion = getFirstExpectedCompletion(schedule)
        val nextExpectedCompletion = if (latestCompletionDueTime != null) latestCompletionDueTime.plusMillis(repeatIntervalMillis) else firstScheduledCompletion
        val sequenceStart = if (nextExpectedCompletion.isBefore(until)) nextExpectedCompletion.plusMillis(repeatIntervalMillis) else null

        return sequenceOf(nextExpectedCompletion).plus(generateSequence(
            sequenceStart,
            {
                val nextElement = it.plusMillis(repeatIntervalMillis)
                if (nextElement.isBefore(until)) return@generateSequence nextElement
                else return@generateSequence null
            }
        ))
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