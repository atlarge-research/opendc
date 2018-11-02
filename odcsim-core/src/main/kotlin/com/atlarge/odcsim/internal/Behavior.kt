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

package com.atlarge.odcsim.internal

import com.atlarge.odcsim.ActorContext
import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.ReceivingBehavior
import com.atlarge.odcsim.Signal

/**
 * A [Behavior] object that ignores all messages sent to the actor.
 */
internal object IgnoreBehavior : ReceivingBehavior<Any>() {
    override fun receive(ctx: ActorContext<Any>, msg: Any): Behavior<Any> = this

    override fun receiveSignal(ctx: ActorContext<Any>, signal: Signal): Behavior<Any> = this

    override fun toString() = "Ignore"
}

/**
 * A [Behavior] object that does not handle any message it receives.
 */
internal object EmptyBehavior : ReceivingBehavior<Any>() {
    override fun toString() = "Empty"
}
