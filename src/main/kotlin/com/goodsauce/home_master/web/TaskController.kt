package com.goodsauce.home_master.web

import com.goodsauce.home_master.logic.TaskService
import com.goodsauce.home_master.pages.TaskSummaryPage
import com.goodsauce.home_master.support.LOG
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tasks")
class TaskController(val taskService: TaskService) {

    @PostMapping("/complete/{completionId}")
    fun completeTask(@PathVariable completionId: Int) {
        taskService.complete(completionId)
        LOG.info { "Completed task $completionId" }
    }

    @GetMapping("/summary/{taskId}")
    fun summary(@PathVariable taskId: Int): String {
        val summary = taskService.getRelevantCompletions(taskId)
        return TaskSummaryPage(summary).getHtml()
    }
}
