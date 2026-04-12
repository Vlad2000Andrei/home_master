package com.goodsauce.home_master.data

import com.goodsauce.home_master.logic.TaskCompletionCreation
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TaskCompletionRepositoryTest : DatabaseTestBase() {

    @Autowired
    lateinit var taskCompletionRepository: TaskCompletionRepository

    companion object {
        private val RANGE_START = Instant.parse("2024-06-01T00:00:00Z")
        private val RANGE_END = Instant.parse("2024-06-03T00:00:00Z")
        private val FAR_FUTURE = Instant.parse("9999-12-31T23:59:59Z")

        @JvmStatic
        fun timeDueFilteringCases(): Stream<Arguments> = Stream.of(
            Arguments.of("inside range",       Instant.parse("2024-06-02T10:00:00Z"), 1),
            Arguments.of("on start boundary",  RANGE_START,                           1),
            Arguments.of("on end boundary",    RANGE_END,                             1),
            Arguments.of("before range",       Instant.parse("2024-05-31T10:00:00Z"), 0),
            Arguments.of("after range",        Instant.parse("2024-06-04T10:00:00Z"), 0),
        )
    }

    // --- getByTaskId ---

    @Test
    fun `getByTaskId returns empty list when no completions exist`() {
        val taskId = insertTask("Dishes")

        assertTrue(taskCompletionRepository.getByTaskId(taskId, Instant.EPOCH..FAR_FUTURE).isEmpty())
    }

    @ParameterizedTest(name = "timeDue {0} -> {2} result(s)")
    @MethodSource("timeDueFilteringCases")
    fun `getByTaskId filters by timeDue range`(@Suppress("UNUSED_PARAMETER") label: String, timeDue: Instant, expectedCount: Int) {
        val taskId = insertTask("Dishes")
        val scheduleId = insertTaskSchedule(taskId)
        insertTaskCompletion(taskId, scheduleId, timeDue)

        val completions = taskCompletionRepository.getByTaskId(taskId, RANGE_START..RANGE_END)

        assertEquals(expectedCount, completions.size)
    }

    @Test
    fun `getByTaskId does not return completions belonging to other tasks`() {
        val taskId1 = insertTask("Dishes")
        val taskId2 = insertTask("Laundry")
        val scheduleId1 = insertTaskSchedule(taskId1)
        val scheduleId2 = insertTaskSchedule(taskId2)
        val timeDue = Instant.parse("2024-06-02T10:00:00Z")
        insertTaskCompletion(taskId1, scheduleId1, timeDue)
        insertTaskCompletion(taskId2, scheduleId2, timeDue)

        val completions = taskCompletionRepository.getByTaskId(taskId1, Instant.EPOCH..FAR_FUTURE)

        assertEquals(1, completions.size)
        assertEquals(taskId1, completions.first().task.id)
    }

    // --- insert (single) ---

    @Test
    fun `insert stores a single completion`() {
        val taskId = insertTask("Dishes")
        val scheduleId = insertTaskSchedule(taskId)
        val timeDue = Instant.parse("2024-06-01T10:00:00Z")

        taskCompletionRepository.insert(TaskCompletionCreation(taskId, scheduleId, timeDue))

        val completions = taskCompletionRepository.getByTaskId(taskId, Instant.EPOCH..FAR_FUTURE)
        assertEquals(1, completions.size)
        assertEquals(timeDue, completions.first().timeDue)
        assertNull(completions.first().timeCompleted)
    }

    // --- insert (batch) ---

    @Test
    fun `insert stores multiple completions`() {
        val taskId = insertTask("Dishes")
        val scheduleId = insertTaskSchedule(taskId)
        val t1 = Instant.parse("2024-06-01T10:00:00Z")
        val t2 = Instant.parse("2024-06-02T10:00:00Z")
        val t3 = Instant.parse("2024-06-03T10:00:00Z")
        val creations = listOf(
            TaskCompletionCreation(taskId, scheduleId, t1),
            TaskCompletionCreation(taskId, scheduleId, t2),
            TaskCompletionCreation(taskId, scheduleId, t3),
        )

        taskCompletionRepository.insert(creations)

        val timeDues = taskCompletionRepository.getByTaskId(taskId, Instant.EPOCH..FAR_FUTURE).map { it.timeDue }.toSet()
        assertEquals(setOf(t1, t2, t3), timeDues)
    }

    // --- complete ---

    @Test
    fun `complete sets timeCompleted on the given completion`() {
        val taskId = insertTask("Dishes")
        val scheduleId = insertTaskSchedule(taskId)
        val timeDue = Instant.parse("2024-06-01T10:00:00Z")
        val completionId = insertTaskCompletion(taskId, scheduleId, timeDue)
        val completionTime = Instant.parse("2024-06-01T10:05:00Z")

        taskCompletionRepository.complete(completionId, completionTime)

        val completion = taskCompletionRepository.getByTaskId(taskId, Instant.EPOCH..FAR_FUTURE).first()
        assertEquals(completionTime, completion.timeCompleted)
    }

    @Test
    fun `complete does not affect other completions`() {
        val taskId = insertTask("Dishes")
        val scheduleId = insertTaskSchedule(taskId)
        val id1 = insertTaskCompletion(taskId, scheduleId, Instant.parse("2024-06-01T10:00:00Z"))
        insertTaskCompletion(taskId, scheduleId, Instant.parse("2024-06-02T10:00:00Z"))

        taskCompletionRepository.complete(id1, Instant.parse("2024-06-01T10:05:00Z"))

        val completions = taskCompletionRepository.getByTaskId(taskId, Instant.EPOCH..FAR_FUTURE)
        val untouched = completions.first { it.id != id1 }
        assertNull(untouched.timeCompleted)
    }

    // --- getLastByScheduleIds ---

    @Test
    fun `getLastByScheduleIds returns most recent timeDue per schedule`() {
        val taskId = insertTask("Dishes")
        val scheduleId = insertTaskSchedule(taskId)
        val earlier = Instant.parse("2024-06-01T10:00:00Z")
        val later = Instant.parse("2024-06-05T10:00:00Z")
        insertTaskCompletion(taskId, scheduleId, earlier)
        insertTaskCompletion(taskId, scheduleId, later)

        val result = taskCompletionRepository.getLastByScheduleIds()

        assertEquals(later, result[scheduleId])
    }

    @Test
    fun `getLastByScheduleIds returns separate entries per schedule`() {
        val taskId = insertTask("Dishes")
        val scheduleId1 = insertTaskSchedule(taskId, name = "Morning")
        val scheduleId2 = insertTaskSchedule(taskId, name = "Evening")
        val t1 = Instant.parse("2024-06-01T08:00:00Z")
        val t2 = Instant.parse("2024-06-01T20:00:00Z")
        insertTaskCompletion(taskId, scheduleId1, t1)
        insertTaskCompletion(taskId, scheduleId2, t2)

        val result = taskCompletionRepository.getLastByScheduleIds()

        assertEquals(t1, result[scheduleId1])
        assertEquals(t2, result[scheduleId2])
    }

    @Test
    fun `getLastByScheduleIds returns empty map when no completions exist`() {
        assertTrue(taskCompletionRepository.getLastByScheduleIds().isEmpty())
    }
}
