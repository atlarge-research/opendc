/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

package com.atlarge.opendc.format.trace.gwf

import com.atlarge.opendc.compute.core.image.FlopsApplicationImage
import com.atlarge.opendc.core.User
import com.atlarge.opendc.format.trace.TraceEntry
import com.atlarge.opendc.format.trace.TraceReader
import com.atlarge.opendc.workflows.workload.Job
import com.atlarge.opendc.workflows.workload.Task
import com.atlarge.opendc.workflows.workload.WORKFLOW_TASK_DEADLINE
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/**
 * A [TraceReader] for the Grid Workload Format. See the Grid Workloads Archive (http://gwa.ewi.tudelft.nl/) for more
 * information about the format.
 *
 * Be aware that in the Grid Workload Format, workflows are not required to be ordered by submission time and therefore
 * this reader needs to read the whole trace into memory before an entry can be read. Consider converting the trace to a
 * different format for better performance.
 *
 * @param reader The buffered reader to read the trace with.
 */
class GwfTraceReader(reader: BufferedReader) : TraceReader<Job> {
    /**
     * The internal iterator to use for this reader.
     */
    private val iterator: Iterator<TraceEntry<Job>>

    /**
     * Create a [GwfTraceReader] instance from the specified [File].
     *
     * @param file The file to read from.
     */
    constructor(file: File) : this(file.bufferedReader())

    /**
     * Create a [GwfTraceReader] instance from the specified [InputStream].
     *
     * @param input The input stream to read from.
     */
    constructor(input: InputStream) : this(input.bufferedReader())

    /**
     * Initialize the reader.
     */
    init {
        val entries = mutableMapOf<Long, TraceEntryImpl>()
        val tasks = mutableMapOf<Long, Task>()
        val taskDependencies = mutableMapOf<Task, List<Long>>()

        var workflowIdCol = 0
        var taskIdCol = 0
        var submitTimeCol = 0
        var runtimeCol = 0
        var coreCol = 0
        var dependencyCol = 0

        try {
            reader.lineSequence()
                .filter { line ->
                    // Ignore comments in the trace
                    !line.startsWith("#") && line.isNotBlank()
                }
                .forEachIndexed { idx, line ->
                    val values = line.split(",")

                    // Parse GWF header
                    if (idx == 0) {
                        val header = values.mapIndexed { col, name -> Pair(name.trim(), col) }.toMap()
                        workflowIdCol = header["WorkflowID"]!!
                        taskIdCol = header["JobID"]!!
                        submitTimeCol = header["SubmitTime"]!!
                        runtimeCol = header["RunTime"]!!
                        coreCol = header["NProcs"]!!
                        dependencyCol = header["Dependencies"]!!
                        return@forEachIndexed
                    }

                    val workflowId = values[workflowIdCol].trim().toLong()
                    val taskId = values[taskIdCol].trim().toLong()
                    val submitTime = values[submitTimeCol].trim().toLong()
                    val runtime = max(0, values[runtimeCol].trim().toLong())
                    val cores = values[coreCol].trim().toInt()
                    val dependencies = values[dependencyCol].split(" ")
                        .filter { it.isNotEmpty() }
                        .map { it.trim().toLong() }

                    val flops: Long = 4000 * runtime * cores

                    val entry = entries.getOrPut(workflowId) {
                        TraceEntryImpl(submitTime, Job(UUID(0L, taskId), "<unnamed>", UnnamedUser, HashSet()))
                    }
                    val workflow = entry.workload
                    val task = Task(
                        UUID(0L, taskId), "<unnamed>",
                        FlopsApplicationImage(UUID.randomUUID(), "<unnamed>", emptyMap(), flops, cores),
                        HashSet(),
                        mapOf(WORKFLOW_TASK_DEADLINE to runtime)
                    )
                    entry.submissionTime = min(entry.submissionTime, submitTime)
                    (workflow.tasks as MutableSet<Task>).add(task)
                    tasks[taskId] = task
                    taskDependencies[task] = dependencies
                }
        } finally {
            reader.close()
        }

        // Fix dependencies and dependents for all tasks
        taskDependencies.forEach { (task, dependencies) ->
            (task.dependencies as MutableSet<Task>).addAll(dependencies.map { taskId ->
                tasks[taskId] ?: throw IllegalArgumentException("Dependency task with id $taskId not found")
            })
        }

        // Create the entry iterator
        iterator = entries.values.sortedBy { it.submissionTime }.iterator()
    }

    override fun hasNext(): Boolean = iterator.hasNext()

    override fun next(): TraceEntry<Job> = iterator.next()

    override fun close() {}

    /**
     * An unnamed user.
     */
    private object UnnamedUser : User {
        override val name: String = "<unnamed>"
        override val uid: UUID = UUID.randomUUID()
    }

    /**
     * An entry in the trace.
     */
    private data class TraceEntryImpl(
        override var submissionTime: Long,
        override val workload: Job
    ) : TraceEntry<Job>
}
