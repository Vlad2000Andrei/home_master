package com.goodsauce.home_master.logic

import com.goodsauce.home_master.data.TaskCompletionRepository
import com.goodsauce.home_master.data.TaskRepository
import com.goodsauce.home_master.support.LOG
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@Service
class TaskService(
    private val taskRepository: TaskRepository, private val taskCompletionRepository: TaskCompletionRepository
) {
    private val zoneId: ZoneId = ZoneId.of("Europe/Amsterdam")
    private val clock = Clock.system(zoneId)

    fun complete(taskId: Int): Unit {
        val completionTime = Instant.now(clock)
        taskCompletionRepository.complete(taskId, completionTime)
        LOG.info { "Completed task with id $taskId at $completionTime" }
    }
}