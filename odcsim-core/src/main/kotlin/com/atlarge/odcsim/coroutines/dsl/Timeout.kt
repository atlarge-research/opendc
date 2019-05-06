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

package com.atlarge.odcsim.coroutines.dsl

import com.atlarge.odcsim.Duration
import com.atlarge.odcsim.Timeout
import com.atlarge.odcsim.coroutines.SuspendingActorContext
import com.atlarge.odcsim.coroutines.suspendWithBehavior
import com.atlarge.odcsim.internal.sendSignal
import com.atlarge.odcsim.receiveSignal
import com.atlarge.odcsim.setup
import com.atlarge.odcsim.unhandled
import kotlin.coroutines.resume

/**
 * Block execution for the specified duration.
 *
 * @param after The duration after which execution should continue.
 */
suspend fun <T : Any> SuspendingActorContext<T>.timeout(after: Duration) =
    suspendWithBehavior<T, Unit> { cont, next ->
        setup { ctx ->
            val target = this
            @Suppress("UNCHECKED_CAST")
            ctx.sendSignal(ctx.self, Timeout(target), after)
            receiveSignal { _, signal ->
                if (signal is Timeout && signal.target == target) {
                    cont.resume(Unit)
                    next()
                } else {
                    unhandled()
                }
            }
        }
    }
