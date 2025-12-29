package com.goodsauce.home_master.web

import com.goodsauce.home_master.logic.TaskService
import com.goodsauce.home_master.logic.TaskSummary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tasks")
class TaskController(val taskService: TaskService) {

    @GetMapping("/complete/{completionId}")
    fun completeTask(@PathVariable completionId: Int) {
        taskService.complete(completionId)
    }

    @GetMapping("/summary/{taskId}")
    fun summary(@PathVariable taskId: Int): TaskSummary {
        return taskService.getRelevantCompletions(taskId)
    }
}
