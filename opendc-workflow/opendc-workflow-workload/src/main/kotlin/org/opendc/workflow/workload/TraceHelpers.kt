/*
 * Copyright (c) 2021 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

@file:JvmName("TraceHelpers")
package org.opendc.workflow.workload

import org.opendc.simulator.compute.workload.SimFlopsWorkload
import org.opendc.trace.*
import org.opendc.trace.conv.*
import org.opendc.workflow.api.Job
import org.opendc.workflow.api.Task
import org.opendc.workflow.api.WORKFLOW_TASK_CORES
import org.opendc.workflow.api.WORKFLOW_TASK_DEADLINE
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.min

/**
 * Convert [Trace] into a list of [Job]s that can be submitted to the workflow service.
 */
public fun Trace.toJobs(): List<Job> {
    val table = checkNotNull(getTable(TABLE_TASKS))
    val reader = table.newReader()

    val jobs = mutableMapOf<Long, Job>()
    val tasks = mutableMapOf<Long, Task>()
    val taskDependencies = mutableMapOf<Task, Set<Long>>()

    try {
        while (reader.nextRow()) {
            // Bag of tasks without workflow ID all share the same workflow
            val workflowId = if (reader.resolve(TASK_WORKFLOW_ID) != -1) reader.getString(TASK_WORKFLOW_ID)!!.toLong() else 0L
            val workflow = jobs.computeIfAbsent(workflowId) { id -> Job(UUID(0L, id), "<unnamed>", HashSet(), HashMap()) }

            val id = reader.getString(TASK_ID)!!.toLong()
            val grantedCpus = if (reader.resolve(TASK_ALLOC_NCPUS) != 0)
                reader.getInt(TASK_ALLOC_NCPUS)
            else
                reader.getInt(TASK_REQ_NCPUS)
            val submitTime = reader.getInstant(TASK_SUBMIT_TIME)!!
            val runtime = reader.getDuration(TASK_RUNTIME)!!
            val flops: Long = 4000 * runtime.seconds * grantedCpus
            val workload = SimFlopsWorkload(flops)
            val task = Task(
                UUID(0L, id),
                "<unnamed>",
                HashSet(),
                mapOf(
                    "workload" to workload,
                    WORKFLOW_TASK_CORES to grantedCpus,
                    WORKFLOW_TASK_DEADLINE to runtime.toMillis()
                ),
            )

            tasks[id] = task
            taskDependencies[task] = reader.getSet(TASK_PARENTS, String::class.java)!!.map { it.toLong() }.toSet()

            (workflow.metadata as MutableMap<String, Any>).merge("WORKFLOW_SUBMIT_TIME", submitTime.toEpochMilli()) { a, b -> min(a as Long, b as Long) }
            (workflow.tasks as MutableSet<Task>).add(task)
        }

        // Resolve dependencies for all tasks
        for ((task, deps) in taskDependencies) {
            for (dep in deps) {
                val parent = requireNotNull(tasks[dep]) { "Dependency task with id $dep not found" }
                (task.dependencies as MutableSet<Task>).add(parent)
            }
        }
    } finally {
        reader.close()
    }

    return jobs.values.toList()
}
