/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.runner.executor

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.yield
import org.opendc.compute.api.TaskState
import org.opendc.compute.simulator.TaskWatcher
import org.opendc.compute.simulator.service.ComputeService
import org.opendc.compute.simulator.service.ServiceTask
import org.opendc.sdk.model.resource.ResourceReference
import org.opendc.sdk.runner.factory.toEngine
import java.nio.file.Path
import java.time.InstantSource
import java.util.Random
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import org.opendc.sdk.model.failure.FailureModel as SdkFailureModel

/**
 * Submits [trace] to this [ComputeService] on the simulated [clock], honouring each task's
 * submission time, and injecting the failures described by [failureModel]. Blocks (in virtual time)
 * until every task reaches [TaskState.DELETED].
 *
 * A decoupled fork of the experiments-base replayer that sources failures from the SDK model
 * instead of experiment specs.
 */
internal suspend fun ComputeService.replay(
    clock: InstantSource,
    trace: List<ServiceTask>,
    failureModel: SdkFailureModel,
    seed: Long,
    resolve: (ResourceReference) -> Path,
    submitImmediately: Boolean = false,
) {
    val client = newClient()
    val engineFailure = failureModel.toEngine(coroutineContext, clock, this, Random(seed), resolve)
    try {
        coroutineScope {
            engineFailure?.start()
            var simulationOffset = Long.MIN_VALUE
            for (task in trace.sortedBy { it.submittedAt }) {
                val now = clock.millis()
                val start = task.submittedAt
                if (simulationOffset == Long.MIN_VALUE) simulationOffset = start - now
                if (!submitImmediately) {
                    delay(max(0, start - now - simulationOffset))
                    task.deadline -= simulationOffset
                }
                launch {
                    val submitted = client.newTask(task)
                    val watcher = RunningTaskWatcher()
                    watcher.lock()
                    submitted.watch(watcher)
                    watcher.await()
                }
            }
        }
        yield()
    } finally {
        engineFailure?.close()
        client.close()
    }
}

/** Blocks until the watched task reaches a terminal state. */
internal class RunningTaskWatcher : TaskWatcher {
    private val unlockStates: List<TaskState> = listOf(TaskState.DELETED)
    private val mutex: Mutex = Mutex()

    suspend fun lock() {
        mutex.lock()
    }

    suspend fun await() {
        lock()
    }

    override fun onStateChanged(
        task: ServiceTask,
        newState: TaskState,
    ) {
        if (unlockStates.contains(newState)) mutex.unlock()
    }
}
