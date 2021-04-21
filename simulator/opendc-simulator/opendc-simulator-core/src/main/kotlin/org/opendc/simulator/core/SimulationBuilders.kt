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

package org.opendc.simulator.core

import kotlinx.coroutines.*
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Executes a [body] inside an immediate execution dispatcher.
 */
@OptIn(ExperimentalCoroutinesApi::class)
public fun runBlockingSimulation(context: CoroutineContext = EmptyCoroutineContext, body: suspend SimulationCoroutineScope.() -> Unit) {
    val (safeContext, dispatcher) = context.checkArguments()
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
 * Convenience method for calling [runBlockingSimulation] on an existing [SimulationCoroutineScope].
 */
public fun SimulationCoroutineScope.runBlockingSimulation(block: suspend SimulationCoroutineScope.() -> Unit): Unit =
    runBlockingSimulation(coroutineContext, block)

/**
 * Convenience method for calling [runBlockingSimulation] on an existing [SimulationCoroutineDispatcher].
 */
public fun SimulationCoroutineDispatcher.runBlockingSimulation(block: suspend SimulationCoroutineScope.() -> Unit): Unit =
    runBlockingSimulation(this, block)

private fun CoroutineContext.checkArguments(): Pair<CoroutineContext, SimulationController> {
    val dispatcher = get(ContinuationInterceptor).run {
        this?.let { require(this is SimulationController) { "Dispatcher must implement SimulationController: $this" } }
        this ?: SimulationCoroutineDispatcher()
    }

    val job = get(Job) ?: SupervisorJob()
    return Pair(this + dispatcher + job, dispatcher as SimulationController)
}

private fun CoroutineContext.activeJobs(): Set<Job> {
    return checkNotNull(this[Job]).children.filter { it.isActive }.toSet()
}
