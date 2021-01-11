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

package org.opendc.experiments.sc18

import org.opendc.trace.core.EventStream
import org.opendc.trace.core.onEvent
import org.opendc.workflows.service.WorkflowEvent
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * This function collects the makespan of workflows that appear in the event stream.
 */
public suspend fun EventStream.workflowRuntime(): Map<UUID, Long> = suspendCoroutine { cont ->
    val starts = mutableMapOf<UUID, Long>()
    val results = mutableMapOf<UUID, Long>()

    onEvent<WorkflowEvent.JobStarted> {
        starts[it.job.uid] = it.timestamp
    }
    onEvent<WorkflowEvent.JobFinished> {
        val start = starts.remove(it.job.uid) ?: return@onEvent
        results[it.job.uid] = it.timestamp - start
    }
    onClose { cont.resume(results) }
}

/**
 * This function collects the waiting time of workflows that appear in the event stream, which the duration between the
 * workflow submission and the start of the first task.
 */
public suspend fun EventStream.workflowWaitingTime(): Map<UUID, Long> = suspendCoroutine { cont ->
    val starts = mutableMapOf<UUID, Long>()
    val results = mutableMapOf<UUID, Long>()

    onEvent<WorkflowEvent.JobStarted> {
        starts[it.job.uid] = it.timestamp
    }
    onEvent<WorkflowEvent.TaskStarted> {
        results.computeIfAbsent(it.job.uid) { _ ->
            val start = starts.remove(it.job.uid)!!
            it.timestamp - start
        }
    }
    onClose { cont.resume(results) }
}

/**
 * This function collects the response time of tasks that appear in the event stream.
 */
public suspend fun EventStream.taskResponse(): Map<UUID, Long> = suspendCoroutine { cont ->
    val starts = mutableMapOf<UUID, Long>()
    val results = mutableMapOf<UUID, Long>()

    onEvent<WorkflowEvent.JobSubmitted> {
        for (task in it.job.tasks) {
            starts[task.uid] = it.timestamp
        }
    }
    onEvent<WorkflowEvent.TaskFinished> {
        val start = starts.remove(it.job.uid) ?: return@onEvent
        results[it.task.uid] = it.timestamp - start
    }
    onClose { cont.resume(results) }
}
