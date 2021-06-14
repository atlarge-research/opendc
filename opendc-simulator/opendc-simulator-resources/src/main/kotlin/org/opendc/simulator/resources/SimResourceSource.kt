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

package org.opendc.simulator.resources

import kotlin.math.ceil
import kotlin.math.min

/**
 * A [SimResourceSource] represents a source for some resource that provides bounded processing capacity.
 *
 * @param initialCapacity The initial capacity of the resource.
 * @param interpreter The interpreter that is used for managing the resource contexts.
 * @param parent The parent resource system.
 */
public class SimResourceSource(
    initialCapacity: Double,
    private val interpreter: SimResourceInterpreter,
    private val parent: SimResourceSystem? = null
) : SimAbstractResourceProvider(interpreter, parent, initialCapacity) {
    override fun createLogic(): SimResourceProviderLogic {
        return object : SimResourceProviderLogic {
            override fun onIdle(ctx: SimResourceControllableContext, deadline: Long): Long {
                return deadline
            }

            override fun onConsume(ctx: SimResourceControllableContext, work: Double, limit: Double, deadline: Long): Long {
                return if (work.isInfinite()) {
                    Long.MAX_VALUE
                } else {
                    min(deadline, ctx.clock.millis() + getDuration(work, speed))
                }
            }

            override fun onUpdate(ctx: SimResourceControllableContext, work: Double) {
                updateCounters(ctx, work)
            }

            override fun onFinish(ctx: SimResourceControllableContext) {
                cancel()
            }
        }
    }

    override fun toString(): String = "SimResourceSource[capacity=$capacity]"

    /**
     * Compute the duration that a resource consumption will take with the specified [speed].
     */
    private fun getDuration(work: Double, speed: Double): Long {
        return ceil(work / speed * 1000).toLong()
    }
}
