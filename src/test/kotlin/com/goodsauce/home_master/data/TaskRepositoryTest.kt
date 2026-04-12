package com.goodsauce.home_master.data

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskRepositoryTest : DatabaseTestBase() {

    @Autowired
    lateinit var taskRepository: TaskRepository

    @Test
    fun `getAll returns empty set when no tasks exist`() {
        assertTrue(taskRepository.getAll().isEmpty())
    }

    @Test
    fun `getAll returns all tasks`() {
        insertTask("Dishes")
        insertTask("Laundry")

        val tasks = taskRepository.getAll()

        assertEquals(2, tasks.size)
        assertTrue(tasks.any { it.name == "Dishes" })
        assertTrue(tasks.any { it.name == "Laundry" })
    }

    @Test
    fun `getById returns the correct task`() {
        val id = insertTask("Vacuuming")

        val task = taskRepository.getById(id)

        assertEquals(id, task.id)
        assertEquals("Vacuuming", task.name)
    }

    @Test
    fun `getById throws when task does not exist`() {
        assertThrows<Exception> {
            taskRepository.getById(999)
        }
    }
}
