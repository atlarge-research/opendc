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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import org.opendc.simulator.SimulationScheduler
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
 * body some other way, or use commands that control scheduling (see [SimulationScheduler]).
 */
@OptIn(ExperimentalCoroutinesApi::class)
public fun runSimulation(
    context: CoroutineContext = EmptyCoroutineContext,
    scheduler: SimulationScheduler = SimulationScheduler(),
    body: suspend SimulationCoroutineScope.() -> Unit
) {
    val (safeContext, dispatcher) = context.checkArguments(scheduler)
    val startingJobs = safeContext.activeJobs()
    val scope = SimulationCoroutineScope(safeContext)
    val deferred = scope.async {
        body(scope)
    }
    dispatcher.advanceUntilIdle()
    deferred.getCompletionExceptionOrNull()?.let {
        throw it
    }
    val endingJobs = safeContext.activeJobs()
    if ((endingJobs - startingJobs).isNotEmpty()) {
        throw IllegalStateException("Test finished with active jobs: $endingJobs")
    }
}

/**
 * Convenience method for calling [runSimulation] on an existing [SimulationCoroutineScope].
 */
public fun SimulationCoroutineScope.runSimulation(block: suspend SimulationCoroutineScope.() -> Unit): Unit =
    runSimulation(coroutineContext, scheduler, block)

/**
 * Convenience method for calling [runSimulation] on an existing [SimulationCoroutineDispatcher].
 */
public fun SimulationCoroutineDispatcher.runSimulation(block: suspend SimulationCoroutineScope.() -> Unit): Unit =
    runSimulation(this, scheduler, block)

private fun CoroutineContext.checkArguments(scheduler: SimulationScheduler): Pair<CoroutineContext, SimulationController> {
    val dispatcher = get(ContinuationInterceptor).run {
        this?.let { require(this is SimulationController) { "Dispatcher must implement SimulationController: $this" } }
        this ?: SimulationCoroutineDispatcher(scheduler)
    }

    val job = get(Job) ?: SupervisorJob()
    return Pair(this + dispatcher + job, dispatcher as SimulationController)
}

private fun CoroutineContext.activeJobs(): Set<Job> {
    return checkNotNull(this[Job]).children.filter { it.isActive }.toSet()
}
