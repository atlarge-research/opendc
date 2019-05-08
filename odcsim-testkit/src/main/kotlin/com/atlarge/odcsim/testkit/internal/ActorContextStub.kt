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

package com.atlarge.odcsim.testkit.internal

import com.atlarge.odcsim.ActorContext
import com.atlarge.odcsim.ActorRef
import com.atlarge.odcsim.ActorSystem
import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.Duration
import com.atlarge.odcsim.Instant
import com.atlarge.odcsim.internal.logging.LoggerImpl
import org.slf4j.Logger
import java.util.UUID

/**
 * A stubbed [ActorContext] implementation for synchronous behavior testing.
 *
 * @property owner The owner of this context.
 */
internal class ActorContextStub<T : Any>(private val owner: BehaviorTestKitImpl<T>) : ActorContext<T> {
    /**
     * The children of this context.
     */
    val children = HashMap<String, BehaviorTestKitImpl<*>>()

    override val self: ActorRef<T>
        get() = owner.ref

    override val time: Instant
        get() = owner.time

    override val system: ActorSystem<*> by lazy {
        ActorSystemStub(owner)
    }

    override val log: Logger by lazy {
        LoggerImpl(this)
    }

    override fun <U : Any> send(ref: ActorRef<U>, msg: U, after: Duration) {
        if (ref !is TestInboxImpl.ActorRefImpl) {
            throw IllegalArgumentException("The referenced ActorRef is not part of the test kit")
        }

        ref.send(msg, after)
    }

    override fun <U : Any> spawn(behavior: Behavior<U>, name: String): ActorRef<U> {
        val btk = BehaviorTestKitImpl(behavior, self.path.child(name))
        children[name] = btk
        return btk.ref
    }

    override fun <U : Any> spawnAnonymous(behavior: Behavior<U>): ActorRef<U> {
        return spawn(behavior, "$" + UUID.randomUUID())
    }

    override fun stop(child: ActorRef<*>): Boolean {
        if (child.path.parent != self.path) {
            // This is not a child of this actor
            return false
        }
        children -= child.path.name
        return true
    }

    override fun watch(target: ActorRef<*>) {}

    override fun unwatch(target: ActorRef<*>) {}

    override fun sync(target: ActorRef<*>) {}

    override fun unsync(target: ActorRef<*>) {}

    override fun isSync(target: ActorRef<*>): Boolean = true
}
