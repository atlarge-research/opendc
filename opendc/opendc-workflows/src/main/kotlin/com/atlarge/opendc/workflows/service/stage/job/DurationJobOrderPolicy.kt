/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.workflows.service.stage.job

import com.atlarge.opendc.workflows.service.JobState
import com.atlarge.opendc.workflows.service.StageWorkflowSchedulerListener
import com.atlarge.opendc.workflows.service.StageWorkflowService
import com.atlarge.opendc.workflows.workload.Job
import com.atlarge.opendc.workflows.workload.Task
import com.atlarge.opendc.workflows.workload.WORKFLOW_TASK_DEADLINE

/**
 * A [JobOrderPolicy] that orders jobs based on its critical path length.
 */
data class DurationJobOrderPolicy(val ascending: Boolean = true) : JobOrderPolicy {
    override fun invoke(scheduler: StageWorkflowService): Comparator<JobState> =
        object : Comparator<JobState>, StageWorkflowSchedulerListener {
            private val results = HashMap<Job, Long>()

            init {
                scheduler.addListener(this)
            }

            private val Job.duration: Long
                get() = results[this]!!

            override fun jobSubmitted(job: JobState) {
                results[job.job] = job.job.toposort().map { task ->
                    val estimable = task.metadata[WORKFLOW_TASK_DEADLINE] as? Long?
                    estimable ?: Long.MAX_VALUE
                }.sum()
            }

            override fun jobFinished(job: JobState) {
                results.remove(job.job)
            }

            override fun compare(o1: JobState, o2: JobState): Int {
                return compareValuesBy(o1, o2) { it.job.duration }.let { if (ascending) it else -it }
            }
        }

    override fun toString(): String {
        return "Job-Duration(${if (ascending) "asc" else "desc"})"
    }
}

/**
 * Create a topological sorting of the tasks in a job.
 *
 * @return The list of tasks within the job topologically sorted.
 */
fun Job.toposort(): List<Task> {
    val res = mutableListOf<Task>()
    val visited = mutableSetOf<Task>()
    val adjacent = mutableMapOf<Task, MutableList<Task>>()

    for (task in tasks) {
        for (dependency in task.dependencies) {
            adjacent.getOrPut(dependency) { mutableListOf() }.add(task)
        }
    }

    fun visit(task: Task) {
        visited.add(task)

        adjacent[task] ?: emptyList<Task>()
            .asSequence()
            .filter { it !in visited }
            .forEach { visit(it) }

        res.add(task)
    }

    tasks
        .asSequence()
        .filter { it !in visited }
        .forEach { visit(it) }
    return res
}
