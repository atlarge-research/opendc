/*
 * Copyright (c) 2022 AtLarge Research
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

@file:JvmName("ScenarioHelpers")

package org.opendc.experiments.base.runner

import CheckpointModelSpec
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.yield
import org.opendc.compute.api.Server
import org.opendc.compute.api.ServerState
import org.opendc.compute.api.ServerWatcher
import org.opendc.compute.failure.models.FailureModel
import org.opendc.compute.service.ComputeService
import org.opendc.compute.workload.VirtualMachine
import org.opendc.experiments.base.scenario.specs.FailureModelSpec
import org.opendc.experiments.base.scenario.specs.createFailureModel
import java.time.InstantSource
import java.util.Random
import kotlin.coroutines.coroutineContext
import kotlin.math.max

/**
 * A watcher that is locked and waits for a change in the server state to unlock
 * @param unlockStates determine which [ServerState] triggers an unlock.
 *                     Default values are TERMINATED, ERROR, and DELETED.
 */
public class RunningServerWatcher : ServerWatcher {
    // TODO: make this changeable
    private val unlockStates: List<ServerState> = listOf(ServerState.DELETED, ServerState.TERMINATED)

    private val mutex: Mutex = Mutex()

    public suspend fun lock() {
        mutex.lock()
    }

    public suspend fun wait() {
        this.lock()
    }

    override fun onStateChanged(
        server: Server,
        newState: ServerState,
    ) {
        if (unlockStates.contains(newState)) {
            mutex.unlock()
        }
    }
}

/**
 * Helper method to replay the specified list of [VirtualMachine] and suspend execution util all VMs have finished.
 *
 * @param clock The simulation clock.
 * @param trace The trace to simulate.
 * @param seed The seed to use for randomness.
 * @param submitImmediately A flag to indicate that the servers are scheduled immediately (so not at their start time).
 * @param failureModelSpec A failure model to use for injecting failures.
 */
public suspend fun ComputeService.replay(
    clock: InstantSource,
    trace: List<VirtualMachine>,
    failureModelSpec: FailureModelSpec? = null,
    checkpointModelSpec: CheckpointModelSpec? = null,
    seed: Long = 0,
    submitImmediately: Boolean = false,
) {
    val client = newClient()

    // Create a failure model based on the failureModelSpec, if not null, otherwise set failureModel to null
    val failureModel: FailureModel? =
        failureModelSpec?.let {
            createFailureModel(coroutineContext, clock, this, Random(seed), it)
        }

    // Create new image for the virtual machine
    val image = client.newImage("vm-image")

    try {
        coroutineScope {
            // Start the fault injector
            failureModel?.start()

            var simulationOffset = Long.MIN_VALUE

            for (entry in trace.sortedBy { it.startTime }) {
                val now = clock.millis()
                val start = entry.startTime.toEpochMilli()

                // Set the simulationOffset based on the starting time of the first server
                if (simulationOffset == Long.MIN_VALUE) {
                    simulationOffset = start - now
                }

                // Delay the server based on the startTime given by the trace.
                if (!submitImmediately) {
                    delay(max(0, (start - now - simulationOffset)))
                }

                val checkpointTime = checkpointModelSpec?.checkpointTime ?: 0L
                val checkpointWait = checkpointModelSpec?.checkpointWait ?: 0L

//                val workload = SimRuntimeWorkload(
//                    entry.duration,
//                    1.0,
//                    checkpointTime,
//                    checkpointWait
//                )

                val workload = entry.trace.createWorkload(start, checkpointTime, checkpointWait)
                val meta = mutableMapOf<String, Any>("workload" to workload)

                launch {
                    val server =
                        client.newServer(
                            entry.name,
                            image,
                            client.newFlavor(
                                entry.name,
                                entry.cpuCount,
                                entry.memCapacity,
                                meta = if (entry.cpuCapacity > 0.0) mapOf("cpu-capacity" to entry.cpuCapacity) else emptyMap(),
                            ),
                            meta = meta,
                        )

                    val serverWatcher = RunningServerWatcher()
                    serverWatcher.lock()
                    server.watch(serverWatcher)

                    // Wait until the server is terminated
                    serverWatcher.wait()

                    // Stop the server after reaching the end-time of the virtual machine
                    server.delete()
                }
            }
        }
        yield()
    } finally {
        failureModel?.close()
        client.close()
    }
}
