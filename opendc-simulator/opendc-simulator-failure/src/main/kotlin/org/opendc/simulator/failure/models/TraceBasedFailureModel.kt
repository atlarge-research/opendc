/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.simulator.failure.models

import kotlinx.coroutines.delay
import org.opendc.simulator.compute.service.ComputeService
import java.time.InstantSource
import java.util.random.RandomGenerator
import kotlin.coroutines.CoroutineContext

/**
 * A [FailureModel] that injects a predefined list of [Failure]s, for example loaded from a failure trace.
 *
 * @param context
 * @param clock
 * @param service
 * @param random
 * @param failures The failures to inject, in order.
 * @param repeat Whether [failures] is replayed from the beginning once exhausted.
 */
public class TraceBasedFailureModel(
    context: CoroutineContext,
    clock: InstantSource,
    service: ComputeService,
    random: RandomGenerator,
    private val failures: List<Failure>,
    private val repeat: Boolean = true,
) : FailureModel(context, clock, service, random) {
    override suspend fun runInjector() {
        // Repeating an empty list would loop forever without suspending, stalling the simulation
        if (failures.isEmpty()) {
            return
        }

        do {
            for (failure in failures) {
                delay(failure.failureInterval)

                val victims = victimSelector.select(hosts, failure.failureIntensity)

                fault.apply(victims, failure.failureDuration)
            }
        } while (repeat)
    }
}
