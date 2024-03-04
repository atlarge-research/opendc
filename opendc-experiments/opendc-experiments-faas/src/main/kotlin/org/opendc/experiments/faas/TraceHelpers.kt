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

package org.opendc.experiments.faas

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.opendc.faas.service.FaaSService
import java.time.InstantSource
import kotlin.math.max

/**
 * Run a simulation of the [FaaSService] by replaying the workload trace given by [trace].
 *
 * @param clock An [InstantSource] instance tracking simulation time.
 * @param trace The trace to simulate.
 */
public suspend fun FaaSService.replay(
    clock: InstantSource,
    trace: List<FunctionTrace>,
) {
    val client = newClient()
    try {
        coroutineScope {
            for (entry in trace) {
                launch {
                    val workload = FunctionTraceWorkload(entry)
                    val function = client.newFunction(entry.id, entry.maxMemory.toLong(), meta = mapOf("workload" to workload))

                    var offset = Long.MIN_VALUE

                    for (sample in entry.samples) {
                        if (sample.invocations == 0) {
                            continue
                        }

                        if (offset < 0) {
                            offset = sample.timestamp - clock.millis()
                        }

                        delay(max(0, (sample.timestamp - offset) - clock.millis()))

                        repeat(sample.invocations) {
                            function.invoke()
                        }
                    }
                }
            }
        }
    } finally {
        client.close()
    }
}
