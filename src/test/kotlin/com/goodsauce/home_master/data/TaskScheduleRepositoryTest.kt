package com.goodsauce.home_master.data

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskScheduleRepositoryTest : DatabaseTestBase() {

    @Autowired
    lateinit var taskScheduleRepository: TaskScheduleRepository

    @Test
    fun `getAll returns empty set when no schedules exist`() {
        assertTrue(taskScheduleRepository.getAll().isEmpty())
    }

    @Test
    fun `getAll returns all schedules across tasks`() {
        val taskId1 = insertTask("Dishes")
        val taskId2 = insertTask("Laundry")
        insertTaskSchedule(taskId1, name = "Morning Dishes")
        insertTaskSchedule(taskId2, name = "Weekly Laundry", repeatRate = "WEEKLY")

        val schedules = taskScheduleRepository.getAll()

        assertEquals(2, schedules.size)
        assertTrue(schedules.any { it.name == "Morning Dishes" })
        assertTrue(schedules.any { it.name == "Weekly Laundry" })
    }

    @Test
    fun `getByTaskId returns schedules for the given task`() {
        val taskId = insertTask("Dishes")
        insertTaskSchedule(taskId, name = "Morning Dishes")
        insertTaskSchedule(taskId, name = "Evening Dishes")

        val schedules = taskScheduleRepository.getByTaskId(taskId)

        assertEquals(2, schedules.size)
        assertTrue(schedules.any { it.name == "Morning Dishes" })
        assertTrue(schedules.any { it.name == "Evening Dishes" })
    }

    @Test
    fun `getByTaskId does not return schedules belonging to other tasks`() {
        val taskId1 = insertTask("Dishes")
        val taskId2 = insertTask("Laundry")
        insertTaskSchedule(taskId1, name = "Morning Dishes")
        insertTaskSchedule(taskId2, name = "Weekly Laundry")

        val schedules = taskScheduleRepository.getByTaskId(taskId1)

        assertEquals(1, schedules.size)
        assertEquals("Morning Dishes", schedules.first().name)
    }

    @Test
    fun `getByTaskId returns empty set when task has no schedules`() {
        val taskId = insertTask("Dishes")

        assertTrue(taskScheduleRepository.getByTaskId(taskId).isEmpty())
    }
}
