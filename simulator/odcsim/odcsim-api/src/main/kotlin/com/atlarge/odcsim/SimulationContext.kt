/*
 * MIT License
 *
 * Copyright (c) 2018 atlarge-research
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

package com.atlarge.odcsim

import java.time.Clock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import org.slf4j.Logger

/**
 * Represents the execution context of a simulation domain.
 */
public interface SimulationContext : CoroutineContext.Element {
    /**
     * Key for [SimulationContext] instance in the coroutine context.
     */
    companion object Key : CoroutineContext.Key<SimulationContext>

    /**
     * The reference to the current simulation domain.
     */
    public val domain: Domain

    /**
     * The clock tracking the simulation time.
     */
    public val clock: Clock

    /**
     * A logger instance tied to the logical process.
     */
    public val log: Logger
}

/**
 * The simulation context of the current coroutine.
 */
@Suppress("WRONG_MODIFIER_TARGET")
public suspend inline val simulationContext: SimulationContext
    @Suppress("ILLEGAL_SUSPEND_PROPERTY_ACCESS")
    get() = coroutineContext[SimulationContext] ?: throw IllegalStateException("No simulation context available")
