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

import com.atlarge.odcsim.ActorPath
import com.atlarge.odcsim.ActorRef
import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.Instant
import com.atlarge.odcsim.internal.BehaviorInterpreter
import com.atlarge.odcsim.testkit.BehaviorTestKit
import com.atlarge.odcsim.testkit.TestInbox
import java.util.UUID
import kotlin.math.max

/**
 * Default implementation of the [BehaviorTestKit] interface.
 *
 * @param initialBehavior The initial behavior to initialize the actor of the test kit with.
 * @param path The path to the actor.
 */
internal class BehaviorTestKitImpl<T : Any>(
    initialBehavior: Behavior<T>,
    path: ActorPath
) : BehaviorTestKit<T> {
    /**
     * The [BehaviorInterpreter] used to interpret incoming messages.
     */
    private val interpreter = BehaviorInterpreter(initialBehavior)

    /**
     * A flag to indicate whether the behavior was initially started.
     */
    private var isStarted: Boolean = false

    override var time: Instant = .0
        private set

    override val inbox: TestInbox<T> = TestInboxImpl(this, path)

    override val behavior: Behavior<T> get() = interpreter.behavior

    override val ref: ActorRef<T> = inbox.ref

    override val context: ActorContextStub<T> = ActorContextStub(this)

    override fun run(msg: T): Boolean {
        if (!isStarted) {
            isStarted = true
            interpreter.start(context)
        }

        return interpreter.interpretMessage(context, msg)
    }

    override fun runOne(): Boolean {
        val (delivery, msg) = inbox.receiveEnvelope()
        time = max(time, delivery)
        return run(msg)
    }

    override fun runTo(time: Instant) {
        while (inbox.hasMessages && this.time <= time) {
            runOne()
        }
        this.time = time
    }

    override fun <U : Any> createInbox(): TestInbox<U> {
        return TestInboxImpl(this, ref.path.child(UUID.randomUUID().toString()))
    }

    override fun <U : Any> childInbox(name: String): TestInbox<U> = childTestKit<U>(name).inbox

    override fun <U : Any> childInbox(ref: ActorRef<U>): TestInbox<U> = childTestKit(ref).inbox

    override fun <U : Any> childTestKit(name: String): BehaviorTestKit<U> {
        @Suppress("UNCHECKED_CAST")
        return context.children[ref.path.name] as BehaviorTestKitImpl<U>? ?: throw IllegalArgumentException("$ref is not a child of $this")
    }

    override fun <U : Any> childTestKit(ref: ActorRef<U>): BehaviorTestKit<U> {
        val btk = childTestKit<U>(ref.path.name)

        if (btk.ref != ref) {
            throw IllegalArgumentException("$ref is not a child of $this")
        }

        return btk
    }
}
