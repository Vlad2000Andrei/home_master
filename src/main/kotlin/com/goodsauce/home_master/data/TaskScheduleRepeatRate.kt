package com.goodsauce.home_master.data

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

enum class TaskScheduleRepeatRate(val completionInterval: Duration) {
    DAILY(1.days),
    WEEKLY(7.days)
}