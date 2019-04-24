/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

import com.atlarge.odcsim.ActorRef
import com.atlarge.odcsim.Duration
import com.atlarge.odcsim.coroutines.SuspendingActorContext
import com.atlarge.odcsim.coroutines.suspendWithBehavior
import com.atlarge.odcsim.receiveMessage
import com.atlarge.odcsim.unhandled
import kotlin.coroutines.resume

/**
 * Receive only messages of type [U] and mark all other messages as unhandled.
 *
 * @return The received message.
 */
suspend inline fun <T : Any, reified U : T> SuspendingActorContext<T>.receiveOf(): U =
    suspendWithBehavior<T, U> { cont, next ->
        receiveMessage { msg ->
            if (msg is U) {
                cont.resume(msg)
                next()
            } else {
                unhandled()
            }
        }
    }

/**
 * Send the specified message to the given reference and wait for a reply.
 *
 * @param ref The actor to send the message to.
 * @param after The delay after which the message should be received by the actor.
 * @param transform The block to transform `self` to a message.
 */
suspend inline fun <T : Any, U : Any, reified V : T> SuspendingActorContext<T>.ask(ref: ActorRef<U>,
                                                                                   after: Duration = 0.0,
                                                                                   transform: (ActorRef<T>) -> U): V {
    send(ref, transform(self), after)
    return receiveOf()
}
