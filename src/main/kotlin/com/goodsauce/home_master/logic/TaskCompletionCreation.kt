package com.goodsauce.home_master.logic

import java.time.Instant

data class TaskCompletionCreation(
    val taskId: Int,
    val taskScheduleId: Int,
    val timeDue: Instant
)
