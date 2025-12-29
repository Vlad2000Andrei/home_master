package com.goodsauce.home_master.logic

import java.time.Instant

data class TaskCompletion(
    val id: Int,
    val taskId: Int,
    val taskScheduleId: Int,
    val taskScheduleName: String,
    val timeCompleted: Instant?,
    val timeDue: Instant
)
