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

import org.opendc.simulator.resources.impl.SimResourceInterpreterImpl
import java.time.Clock
import kotlin.coroutines.CoroutineContext

/**
 * The resource interpreter is responsible for managing the interaction between resource consumer and provider.
 *
 * The interpreter centralizes the scheduling logic of state updates of resource context, allowing update propagation
 * to happen more efficiently. and overall, reducing the work necessary to transition into a steady state.
 */
public interface SimResourceInterpreter {
    /**
     * The [Clock] associated with this interpreter.
     */
    public val clock: Clock

    /**
     * Create a new [SimResourceControllableContext] with the given [provider].
     *
     * @param consumer The consumer logic.
     * @param provider The logic of the resource provider.
     * @param parent The system to which the resource context belongs.
     */
    public fun newContext(
        consumer: SimResourceConsumer,
        provider: SimResourceProviderLogic,
        parent: SimResourceSystem? = null
    ): SimResourceControllableContext

    /**
     * Start batching the execution of resource updates until [popBatch] is called.
     *
     * This method is useful if you want to propagate multiple resources updates (e.g., starting multiple CPUs
     * simultaneously) in a single state update.
     *
     * Multiple calls to this method requires the same number of [popBatch] calls in order to properly flush the
     * resource updates. This allows nested calls to [pushBatch], but might cause issues if [popBatch] is not called
     * the same amount of times. To simplify batching, see [batch].
     */
    public fun pushBatch()

    /**
     * Stop the batching of resource updates and run the interpreter on the batch.
     *
     * Note that method will only flush the event once the first call to [pushBatch] has received a [popBatch] call.
     */
    public fun popBatch()

    public companion object {
        /**
         * Construct a new [SimResourceInterpreter] implementation.
         *
         * @param context The coroutine context to use.
         * @param clock The virtual simulation clock.
         */
        @JvmName("create")
        public operator fun invoke(context: CoroutineContext, clock: Clock): SimResourceInterpreter {
            return SimResourceInterpreterImpl(context, clock)
        }
    }
}

/**
 * Batch the execution of several interrupts into a single call.
 *
 * This method is useful if you want to propagate the start of multiple resources (e.g., CPUs) in a single update.
 */
public inline fun SimResourceInterpreter.batch(block: () -> Unit) {
    try {
        pushBatch()
        block()
    } finally {
        popBatch()
    }
}
