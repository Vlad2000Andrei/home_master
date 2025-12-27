package com.goodsauce.home_master.data

import org.ktorm.database.Database
import org.ktorm.dsl.isNotNull
import org.ktorm.entity.Entity
import org.ktorm.entity.map
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.text
import org.springframework.stereotype.Repository

@Repository
class TaskRepository(private val database: Database) {

    object Tasks : Table<Task>("task") {
        val id = int("id").primaryKey().bindTo { it.id }
        val name = text("name").bindTo { it.name }
    }

    interface Task : Entity<Task> {
        companion object : Entity.Factory<Task>()

        val id: Int
        val name: String
    }

    fun getAll(): Set<TaskEntity> = database.sequenceOf(Tasks).map { task -> TaskEntity(task.id, task.name) }.toSet()
}