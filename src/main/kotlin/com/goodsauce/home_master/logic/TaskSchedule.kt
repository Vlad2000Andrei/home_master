package com.goodsauce.home_master.logic

import com.goodsauce.home_master.data.TaskScheduleRepeatRate
import java.time.LocalDate
import java.time.LocalTime

data class TaskSchedule(
    val taskId: Int,
    val repeatRate: TaskScheduleRepeatRate,
    val scheduledTime: LocalTime,
    val startDate: LocalDate,
    val name: String,
)
