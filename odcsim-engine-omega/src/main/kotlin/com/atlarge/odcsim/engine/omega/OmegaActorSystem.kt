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

package com.atlarge.odcsim.engine.omega

import com.atlarge.odcsim.ActorContext
import com.atlarge.odcsim.ActorPath
import com.atlarge.odcsim.ActorRef
import com.atlarge.odcsim.ActorSystem
import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.Duration
import com.atlarge.odcsim.Instant
import com.atlarge.odcsim.PostStop
import com.atlarge.odcsim.PreStart
import com.atlarge.odcsim.Signal
import mu.KotlinLogging
import java.util.PriorityQueue
import kotlin.math.max

/**
 * The reference implementation of the [ActorSystem] instance for the OpenDC simulation core.
 *
 * This engine implementation is a single-threaded implementation, running actors synchronously and
 * provides a single priority queue for all events (messages, ticks, etc) that occur.
 *
 * @param root The behavior of the root actor.
 * @param name The name of the engine instance.
 */
class OmegaActorSystem<in T : Any>(root: Behavior<T>, override val name: String) : ActorSystem<T>, ActorRef<T> {
    /**
     * The current point in simulation time.
     */
    override var time: Instant = .0

    /**
     * The path to the root actor.
     */
    override val path: ActorPath = ActorPath.Root()

    /**
     * The event queue to process
     */
    private val queue: PriorityQueue<Envelope> = PriorityQueue(Comparator
        .comparingDouble(Envelope::time)
        .thenComparingLong(Envelope::id))

    /**
     * The registry of actors in the system.
     */
    private val registry: MutableMap<ActorPath, Actor<*>> = HashMap()

    private val logger = KotlinLogging.logger {}

    override fun run(until: Duration) {
        require(until >= .0) { "The given instant must be a positive number" }

        while (true) {
            val envelope = queue.peek() ?: break
            val delivery = envelope.time.takeUnless { it > until } ?: break

            if (delivery < time) {
                // Message out of order
                logger.warn { "Message delivered out of order [expected=$delivery, actual=$time]" }
            }

            time = delivery
            queue.poll()

            // Notice that messages for unknown/terminated actors are ignored for now
            registry[envelope.destination]?.interpretMessage(envelope.message)
        }

        // Jump forward in time as the caller expects the system to have run until the specified instant
        // Taking the maximum value prevents the caller to jump backwards in time
        time = max(time, until)
    }

    override fun send(msg: T, after: Duration) = schedule(this, msg, after)

    /**
     * The identifier for the next message to be scheduled.
     */
    private var nextId: Long = 0

    init {
        registry[path] = Actor(this, root)
        schedule(this, PreStart, .0)
    }

    private inner class Actor<T : Any>(override val self: ActorRef<T>, var behavior: Behavior<T>) : ActorContext<T> {
        val children: MutableSet<Actor<*>> = mutableSetOf()

        override val time: Instant
            get() = this@OmegaActorSystem.time

        override fun <U : Any> spawn(name: String, behavior: Behavior<U>): ActorRef<U> {
            val ref = ActorRefImpl<U>(self.path.child(name))
            if (ref.path !in registry) {
                val actor = Actor(ref, behavior)
                registry[ref.path] = actor
                children += actor
                schedule(ref, PreStart, .0)
            }
            return ref
        }

        override fun <U : Any> stop(child: ActorRef<U>): Boolean {
            if (child.path.root != this@OmegaActorSystem.path) {
                // This child is not part of the hierarchy.
                return false
            }
            val ref = registry[child.path] ?: return false
            ref.terminate()
            return true
        }

        /**
         * Terminate this actor and its children.
         */
        fun terminate() {
            children.forEach { it.terminate() }
            registry.remove(self.path)
            interpretMessage(PostStop)
        }

        /**
         * Interpret the given message send to an actor. Make sure the message is of the correct type.
         */
        fun interpretMessage(msg: Any) {
            @Suppress("UNCHECKED_CAST")
            behavior = if (msg is Signal) behavior.receiveSignal(this, msg) else behavior.receive(this, msg as T)
        }

        override fun equals(other: Any?): Boolean =
            other is OmegaActorSystem<*>.Actor<*> && self.path == other.self.path

        override fun hashCode(): Int = self.path.hashCode()
    }

    private inner class ActorRefImpl<T : Any>(override val path: ActorPath) : ActorRef<T> {
        override fun send(msg: T, after: Duration) = schedule(this, msg, after)
    }

    /**
     * A wrapper around a message that has been scheduled for processing.
     *
     * @property time The point in time to deliver the message.
     * @property id The identifier of the message to keep the priority queue stable.
     * @property destination The destination of the message.
     * @property message The message to wrap.
     */
    private class Envelope(val time: Instant, val id: Long, val destination: ActorPath, val message: Any)

    /**
     * Schedule a message to be processed by the engine.
     *
     * @property destination The destination of the message.
     * @param message The message to schedule.
     * @param delay The time to wait before processing the message.
     */
    private fun schedule(destination: ActorRef<*>, message: Any, delay: Duration) {
        require(delay >= .0) { "The given delay must be a non-negative number" }
        queue.add(Envelope(time + delay, nextId++, destination.path, message))
    }
}
