/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.format.trace.wtf

import org.apache.avro.generic.GenericRecord
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.AvroParquetReader
import org.opendc.format.trace.TraceEntry
import org.opendc.format.trace.TraceReader
import org.opendc.simulator.compute.workload.SimFlopsWorkload
import org.opendc.workflow.api.Job
import org.opendc.workflow.api.Task
import org.opendc.workflow.api.WORKFLOW_TASK_CORES
import org.opendc.workflow.api.WORKFLOW_TASK_DEADLINE
import java.util.UUID
import kotlin.math.min

/**
 * A [TraceReader] for the Workflow Trace Format (WTF). See the Workflow Trace Archive
 * (https://wta.atlarge-research.com/) for more information about the format.
 *
 * @param path The path to the trace.
 */
public class WtfTraceReader(path: String) : TraceReader<Job> {
    /**
     * The internal iterator to use for this reader.
     */
    private val iterator: Iterator<TraceEntry<Job>>

    /**
     * Initialize the reader.
     */
    init {
        val workflows = mutableMapOf<Long, Job>()
        val starts = mutableMapOf<Long, Long>()
        val tasks = mutableMapOf<Long, Task>()
        val taskDependencies = mutableMapOf<Task, List<Long>>()

        @Suppress("DEPRECATION")
        val reader = AvroParquetReader.builder<GenericRecord>(Path(path, "tasks/schema-1.0")).build()

        while (true) {
            val nextRecord = reader.read() ?: break

            val workflowId = nextRecord.get("workflow_id") as Long
            val taskId = nextRecord.get("id") as Long
            val submitTime = nextRecord.get("ts_submit") as Long
            val runtime = nextRecord.get("runtime") as Long
            val cores = (nextRecord.get("resource_amount_requested") as Double).toInt()
            @Suppress("UNCHECKED_CAST")
            val dependencies = (nextRecord.get("parents") as ArrayList<GenericRecord>).map {
                it.get("item") as Long
            }

            val flops: Long = 4100 * (runtime / 1000) * cores

            val workflow = workflows.getOrPut(workflowId) {
                Job(UUID(0L, workflowId), "<unnamed>", HashSet())
            }
            val workload = SimFlopsWorkload(flops)
            val task = Task(
                UUID(0L, taskId),
                "<unnamed>",
                HashSet(),
                mapOf(
                    "workload" to workload,
                    WORKFLOW_TASK_CORES to cores,
                    WORKFLOW_TASK_DEADLINE to runtime
                )
            )

            starts.merge(workflowId, submitTime, ::min)
            (workflow.tasks as MutableSet<Task>).add(task)
            tasks[taskId] = task
            taskDependencies[task] = dependencies
        }

        // Fix dependencies and dependents for all tasks
        taskDependencies.forEach { (task, dependencies) ->
            (task.dependencies as MutableSet<Task>).addAll(
                dependencies.map { taskId ->
                    tasks[taskId] ?: throw IllegalArgumentException("Dependency task with id $taskId not found")
                }
            )
        }

        // Create the entry iterator
        iterator = workflows.map { (id, job) -> TraceEntry(job.uid, job.name, starts.getValue(id), job, job.metadata) }
            .sortedBy { it.start }
            .iterator()
    }

    override fun hasNext(): Boolean = iterator.hasNext()

    override fun next(): TraceEntry<Job> = iterator.next()

    override fun close() {}
}
