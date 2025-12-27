package com.goodsauce.home_master.web

import com.goodsauce.home_master.data.TaskEntity
import com.goodsauce.home_master.logic.TaskService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tasks")
class TaskController(val taskService: TaskService) {

    @GetMapping("/complete/{taskId}")
    fun completeTask(@PathVariable taskId: Int) {
        taskService.complete(taskId)
    }

    @GetMapping("/count")
    fun listTasks(): Set<TaskEntity> = setOf()
}
