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

package com.atlarge.odcsim.testkit

import com.atlarge.odcsim.ActorContext
import com.atlarge.odcsim.ActorPath
import com.atlarge.odcsim.ActorRef
import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.Instant
import com.atlarge.odcsim.isAlive
import com.atlarge.odcsim.testkit.internal.BehaviorTestKitImpl
import java.util.UUID

/**
 * Utensils for synchronous testing of [Behavior] instances.
 *
 * @param T The shape of the messages the behavior accepts.
 */
interface BehaviorTestKit<T : Any> {
    /**
     * The current point in simulation time.
     */
    val time: Instant

    /**
     * The current behavior of the simulated actor.
     */
    val behavior: Behavior<T>

    /**
     * The self reference to the simulated actor inside the test kit.
     */
    val ref: ActorRef<T>

    /**
     * The context of the simulated actor.
     */
    val context: ActorContext<T>

    /**
     * The inbox of the simulated actor.
     */
    val inbox: TestInbox<T>

    /**
     * A flag indicating whether the [Behavior] instance is still alive.
     */
    val isAlive: Boolean get() = behavior.isAlive

    /**
     * Interpret the specified message at the current point in simulation time using the current behavior.
     *
     * @return `true` if the message was handled by the behavior, `false` if it was unhandled.
     */
    fun run(msg: T): Boolean

    /**
     * Interpret the oldest message in the inbox using the current behavior.
     *
     * @return `true` if the message was handled by the behavior, `false` if it was unhandled.
     * @throws NoSuchElementException if the actor's inbox is empty.
     */
    fun runOne(): Boolean

    /**
     * Interpret the messages in the inbox until the specified point in simulation time is reached.
     *
     * @param time The time until which the messages in the inbox should be processed.
     */
    fun runTo(time: Instant)

    /**
     * Create an anonymous [TestInbox] for receiving messages.
     */
    fun <U : Any> createInbox(): TestInbox<U>

    /**
     * Get the child inbox for the child with the given name, or fail if there is no child with the given name
     * spawned
     */
    fun <U : Any> childInbox(name: String): TestInbox<U>

    /**
     * Get the child inbox for the child referenced by [ref], or fail if it is not a child of this behavior.
     */
    fun <U : Any> childInbox(ref: ActorRef<U>): TestInbox<U>

    /**
     * Obtain the [BehaviorTestKit] for the child with the given name, or fail if there is no child with the given
     * name spawned.
     */
    fun <U : Any> childTestKit(name: String): BehaviorTestKit<U>

    /**
     * Obtain the [BehaviorTestKit] for the given child [ActorRef].
     */
    fun <U : Any> childTestKit(ref: ActorRef<U>): BehaviorTestKit<U>

    companion object {
        /**
         * Create a [BehaviorTestKit] instance for the specified [Behavior].
         *
         * @param behavior The behavior for which a test kit should be created.
         */
        operator fun <T : Any> invoke(behavior: Behavior<T>): BehaviorTestKit<T> =
            BehaviorTestKitImpl(behavior, ActorPath.Root(name = "/" + UUID.randomUUID().toString()))

        /**
         * Create a [BehaviorTestKit] instance for the specified [Behavior].
         *
         * @param behavior The behavior for which a test kit should be created.
         */
        @JvmStatic
        fun <T : Any> create(behavior: Behavior<T>): BehaviorTestKit<T> = invoke(behavior)
    }
}
