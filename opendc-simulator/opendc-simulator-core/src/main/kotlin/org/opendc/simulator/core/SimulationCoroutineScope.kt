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

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 *  A scope which provides detailed control over the execution of coroutines for simulations.
 */
public interface SimulationCoroutineScope : CoroutineScope, SimulationController

private class SimulationCoroutineScopeImpl(
    override val coroutineContext: CoroutineContext
) :
    SimulationCoroutineScope,
    SimulationController by coroutineContext.simulationController

/**
 * A scope which provides detailed control over the execution of coroutines for simulations.
 *
 * If the provided context does not provide a [ContinuationInterceptor] (Dispatcher) or [CoroutineExceptionHandler], the
 * scope adds [SimulationCoroutineDispatcher] automatically.
 */
@Suppress("FunctionName")
public fun SimulationCoroutineScope(context: CoroutineContext = EmptyCoroutineContext): SimulationCoroutineScope {
    var safeContext = context
    if (context[ContinuationInterceptor] == null) safeContext += SimulationCoroutineDispatcher()
    return SimulationCoroutineScopeImpl(safeContext)
}

private inline val CoroutineContext.simulationController: SimulationController
    get() {
        val handler = this[ContinuationInterceptor]
        return handler as? SimulationController ?: throw IllegalArgumentException(
            "SimulationCoroutineScope requires a SimulationController such as SimulatorCoroutineDispatcher as " +
                "the ContinuationInterceptor (Dispatcher)"
        )
    }
