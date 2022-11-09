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

package org.opendc.simulator.kotlin

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.children
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.opendc.common.DispatcherProvider
import org.opendc.common.asCoroutineDispatcher
import org.opendc.simulator.SimulationDispatcher
import java.time.InstantSource
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Executes [body] as a simulation in a new coroutine.
 *
 * This function behaves similarly to [runBlocking], with the difference that the code that it runs will skip delays.
 * This allows to use [delay] in without causing the simulation to take more time than necessary.
 *
 * ```
 * @Test
 * fun exampleSimulation() = runSimulation {
 *     val deferred = async {
 *         delay(1_000)
 *         async {
 *             delay(1_000)
 *         }.await()
 *     }
 *
 *     deferred.await() // result available immediately
 * }
 * ```
 *
 * The simulation is run in a single thread, unless other [CoroutineDispatcher] are used for child coroutines.
 * Because of this, child coroutines are not executed in parallel to [body].
 * In order for the spawned-off asynchronous code to actually be executed, one must either [yield] or suspend the
 * body some other way, or use commands that control scheduling (see [SimulationDispatcher]).
 */
@OptIn(ExperimentalCoroutinesApi::class)
public fun runSimulation(
    context: CoroutineContext = EmptyCoroutineContext,
    scheduler: SimulationDispatcher = SimulationDispatcher(),
    body: suspend SimulationCoroutineScope.() -> Unit
) {
    val (safeContext, job, dispatcher) = context.checkArguments(scheduler)
    val startingJobs = job.activeJobs()
    val scope = SimulationCoroutineScope(safeContext)
    val deferred = scope.async {
        body(scope)
    }
    dispatcher.advanceUntilIdle()
    deferred.getCompletionExceptionOrNull()?.let {
        throw it
    }
    val endingJobs = job.activeJobs()
    if ((endingJobs - startingJobs).isNotEmpty()) {
        throw IllegalStateException("Test finished with active jobs: $endingJobs")
    }
}

/**
 * Convenience method for calling [runSimulation] on an existing [SimulationCoroutineScope].
 */
public fun SimulationCoroutineScope.runSimulation(block: suspend SimulationCoroutineScope.() -> Unit): Unit =
    runSimulation(coroutineContext, dispatcher, block)

private fun CoroutineContext.checkArguments(scheduler: SimulationDispatcher): Triple<CoroutineContext, Job, SimulationController> {
    val job = get(Job) ?: SupervisorJob()
    val dispatcher = get(ContinuationInterceptor) ?: scheduler.asCoroutineDispatcher()
    val simulationDispatcher = dispatcher.asSimulationDispatcher()
    return Triple(this + dispatcher + job, job, simulationDispatcher.asController())
}

private fun Job.activeJobs(): Set<Job> {
    return children.filter { it.isActive }.toSet()
}

/**
 * Convert a [ContinuationInterceptor] into a [SimulationDispatcher] if possible.
 */
internal fun ContinuationInterceptor.asSimulationDispatcher(): SimulationDispatcher {
    val provider = this as? DispatcherProvider ?: throw IllegalArgumentException(
        "DispatcherProvider such as SimulatorCoroutineDispatcher as the ContinuationInterceptor(Dispatcher) is required"
    )

    return provider.dispatcher as? SimulationDispatcher ?: throw IllegalArgumentException("Active dispatcher is not a SimulationDispatcher")
}

/**
 * Helper method to convert a [SimulationDispatcher] into a [SimulationController].
 */
internal fun SimulationDispatcher.asController(): SimulationController {
    return object : SimulationController {
        override val dispatcher: SimulationDispatcher
            get() = this@asController

        override val timeSource: InstantSource
            get() = this@asController.timeSource

        override fun advanceUntilIdle() {
            dispatcher.advanceUntilIdle()
        }

        override fun advanceBy(delayMs: Long) {
            dispatcher.advanceBy(delayMs)
        }

        override fun runCurrent() {
            dispatcher.runCurrent()
        }
    }
}
