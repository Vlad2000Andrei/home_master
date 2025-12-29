package com.goodsauce.home_master.logic

data class TaskSummary(val task : Task, val completedTasks: List<TaskCompletion>, val dueTasks: List<TaskCompletion>)
