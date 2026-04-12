package com.goodsauce.home_master.data

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.Instant

@SpringBootTest(properties = ["server.port=8080"])
abstract class DatabaseTestBase {

    companion object {
        private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16").also { it.start() }

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE task_completion, task_schedule, task RESTART IDENTITY CASCADE")
    }

    // --- Helpers ---

    fun insertTask(name: String): Int =
        jdbcTemplate.queryForObject(
            "INSERT INTO task (name) VALUES (?) RETURNING id", Int::class.java, name
        )!!

    fun insertTaskSchedule(
        taskId: Int,
        repeatRate: String = "DAILY",
        scheduledTime: String = "09:00:00",
        startDate: String = "2024-01-01",
        name: String = "Test Schedule"
    ): Int =
        jdbcTemplate.queryForObject(
            "INSERT INTO task_schedule (task_id, repeat_rate, scheduled_time, start_date, name) VALUES (?, ?, ?, ?, ?) RETURNING id",
            Int::class.java, taskId, repeatRate, Time.valueOf(scheduledTime), Date.valueOf(startDate), name
        )!!

    fun insertTaskCompletion(
        taskId: Int,
        taskScheduleId: Int,
        timeDue: Instant,
        timeCompleted: Instant? = null
    ): Int = if (timeCompleted != null) {
        jdbcTemplate.queryForObject(
            "INSERT INTO task_completion (task_id, task_schedule_id, time_due, time_completed) VALUES (?, ?, ?, ?) RETURNING id",
            Int::class.java, taskId, taskScheduleId, Timestamp.from(timeDue), Timestamp.from(timeCompleted)
        )!!
    } else {
        jdbcTemplate.queryForObject(
            "INSERT INTO task_completion (task_id, task_schedule_id, time_due) VALUES (?, ?, ?) RETURNING id",
            Int::class.java, taskId, taskScheduleId, Timestamp.from(timeDue)
        )!!
    }
}
