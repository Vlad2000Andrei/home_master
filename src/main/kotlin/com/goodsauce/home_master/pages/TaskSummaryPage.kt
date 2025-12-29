package com.goodsauce.home_master.pages

import com.goodsauce.home_master.logic.TaskSummary
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import kotlin.time.Clock
import kotlin.time.Instant

class TaskSummaryPage(private val taskSummary: TaskSummary) {

    fun getHtml(): String {
        return createHTML().html {
            head {
                title(taskSummary.task.name)
                meta(name = "viewport", content = "width=device-width, initial-scale=1")
                link(rel = "stylesheet", href = "https://cdn.jsdelivr.net/npm/@picocss/pico@1/css/pico.min.css")
                script(src = "https://unpkg.com/htmx.org@1.9.10") {}
            }
            body {
                main(classes = "container") {
                    h1 { +"Completed" }

                    taskSummary.completedTasks.forEach { completedTask ->
                        button(classes = "secondary") {
                            attributes["hx-swap"] = "none"
                            +"${completedTask.taskScheduleName}\n(Due ${prettyPrintInstantRelative(completedTask.timeDue)})"
                        }
                    }

                    h1 { +"Due Soon" }

                    taskSummary.dueTasks.forEach { dueTask ->
                        button {
                            attributes["hx-post"] = "/api/tasks/complete/${dueTask.id}"
                            attributes["hx-swap"] = "none"
                            attributes["hx-on::after-request"] = "window.location.reload()"
                            +"${dueTask.taskScheduleName}\n(Due ${prettyPrintInstantRelative(dueTask.timeDue)})"
                        }
                    }
                }
            }
        }
    }

    private fun prettyPrintInstantRelative(time: java.time.Instant) =
        prettyPrintInstantRelative(Instant.fromEpochMilliseconds(time.toEpochMilli()))

    private fun prettyPrintInstantRelative(time: Instant): String {
        val now = Clock.System.now()
        val timeDifference = now - time
        var timeToDisplay = ""

        if (timeDifference.absoluteValue.inWholeSeconds < 1) {
            timeToDisplay = "${timeDifference.absoluteValue.inWholeSeconds} seconds"
        } else if (timeDifference.absoluteValue.inWholeHours < 1) {
            timeToDisplay = "${timeDifference.absoluteValue.inWholeMinutes} minutes"
        } else if (timeDifference.absoluteValue.inWholeDays < 1) {
            timeToDisplay = "${timeDifference.absoluteValue.inWholeHours} hours"
        } else {
            timeToDisplay =
                "${timeDifference.absoluteValue.inWholeDays} days ${timeDifference.absoluteValue.inWholeHours} hours"
        }

        return if (timeDifference.isNegative()) "in $timeToDisplay" else "$timeToDisplay ago"
    }
}