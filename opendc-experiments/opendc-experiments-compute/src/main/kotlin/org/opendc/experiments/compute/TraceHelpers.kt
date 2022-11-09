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

@file:JvmName("TraceHelpers")

package org.opendc.experiments.compute

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.opendc.compute.service.ComputeService
import java.time.InstantSource
import java.util.Random
import kotlin.coroutines.coroutineContext
import kotlin.math.max

/**
 * Helper method to replay the specified list of [VirtualMachine] and suspend execution util all VMs have finished.
 *
 * @param clock The simulation clock.
 * @param trace The trace to simulate.
 * @param seed The seed to use for randomness.
 * @param submitImmediately A flag to indicate that the servers are scheduled immediately (so not at their start time).
 * @param failureModel A failure model to use for injecting failures.
 * @param interference A flag to indicate that VM interference needs to be enabled.
 */
public suspend fun ComputeService.replay(
    clock: InstantSource,
    trace: List<VirtualMachine>,
    seed: Long,
    submitImmediately: Boolean = false,
    failureModel: FailureModel? = null,
    interference: Boolean = false
) {
    val injector = failureModel?.createInjector(coroutineContext, clock, this, Random(seed))
    val client = newClient()

    // Create new image for the virtual machine
    val image = client.newImage("vm-image")

    try {
        coroutineScope {
            // Start the fault injector
            injector?.start()

            var offset = Long.MIN_VALUE

            for (entry in trace.sortedBy { it.startTime }) {
                val now = clock.millis()
                val start = entry.startTime.toEpochMilli()

                if (offset < 0) {
                    offset = start - now
                }

                // Make sure the trace entries are ordered by submission time
                assert(start - offset >= 0) { "Invalid trace order" }

                if (!submitImmediately) {
                    delay(max(0, (start - offset) - now))
                }

                val workloadOffset = -offset + 300001
                val workload = entry.trace.createWorkload(workloadOffset)
                val meta = mutableMapOf<String, Any>("workload" to workload)

                val interferenceProfile = entry.interferenceProfile
                if (interference && interferenceProfile != null) {
                    meta["interference-profile"] = interferenceProfile
                }

                launch {
                    val server = client.newServer(
                        entry.name,
                        image,
                        client.newFlavor(
                            entry.name,
                            entry.cpuCount,
                            entry.memCapacity,
                            meta = if (entry.cpuCapacity > 0.0) mapOf("cpu-capacity" to entry.cpuCapacity) else emptyMap()
                        ),
                        meta = meta
                    )

                    // Wait for the server reach its end time
                    val endTime = entry.stopTime.toEpochMilli()
                    delay(endTime + workloadOffset - clock.millis() + 5 * 60 * 1000)

                    // Stop the server after reaching the end-time of the virtual machine
                    server.stop()
                }
            }
        }

        yield()
    } finally {
        injector?.close()
        client.close()
    }
}
