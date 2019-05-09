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
import com.atlarge.odcsim.Envelope
import com.atlarge.odcsim.Instant
import com.atlarge.odcsim.PostStop
import com.atlarge.odcsim.PreStart
import com.atlarge.odcsim.Signal
import com.atlarge.odcsim.Terminated
import com.atlarge.odcsim.empty
import com.atlarge.odcsim.internal.BehaviorInterpreter
import com.atlarge.odcsim.internal.logging.LoggerImpl
import com.sun.org.apache.xalan.internal.lib.ExsltDatetime.time
import com.sun.xml.internal.messaging.saaj.soap.impl.EnvelopeImpl
import org.slf4j.Logger
import java.util.Collections
import java.util.PriorityQueue
import java.util.UUID
import java.util.WeakHashMap
import kotlin.math.max

/**
 * The reference implementation of the [ActorSystem] instance for the OpenDC simulation core.
 *
 * This engine implementation is a single-threaded implementation, running actors synchronously and
 * provides a single priority queue for all events (messages, ticks, etc) that occur.
 *
 * @param guardianBehavior The behavior of the guardian (root) actor.
 * @param name The name of the engine instance.
 */
class OmegaActorSystem<in T : Any>(guardianBehavior: Behavior<T>, override val name: String) : ActorSystem<T>, ActorRef<T> {
    /**
     * The state of the actor system.
     */
    private var state: ActorSystemState = ActorSystemState.CREATED

    /**
     * The event queue to process
     */
    private val queue: PriorityQueue<EnvelopeImpl> = PriorityQueue(
        Comparator
            .comparingDouble(EnvelopeImpl::time)
            .thenComparingLong(EnvelopeImpl::id)
    )

    /**
     * The registry of actors in the system.
     */
    private val registry: MutableMap<ActorPath, Actor<*>> = HashMap()

    /**
     * The root actor path of the system.
     */
    private val root: ActorPath = ActorPath.Root()

    /**
     * The system actor path.
     */
    private val system: ActorPath = root / "system"

    /**
     * The current point in simulation time.
     */
    override var time: Instant = .0

    /**
     * The path to the root actor.
     */
    override val path: ActorPath = root / "user"

    init {
        registry[system] = Actor(ActorRefImpl(this, system), empty<Nothing>())
        registry[path] = Actor(this, guardianBehavior)
        schedule(this, PreStart, .0)
    }

    override fun run(until: Duration) {
        require(until >= .0) { "The given instant must be a non-negative number" }

        // Start the system/guardian actor on initial run
        if (state == ActorSystemState.CREATED) {
            state = ActorSystemState.STARTED
            registry[system]!!.isolate { it.start() }
            registry[path]!!.isolate { it.start() }
        } else if (state == ActorSystemState.TERMINATED) {
            throw IllegalStateException("The ActorSystem has been terminated.")
        }

        while (time < until) {
            // Check whether the system was interrupted
            if (Thread.interrupted()) {
                throw InterruptedException()
            }

            val envelope = queue.peek() ?: break
            val delivery = envelope.time.takeUnless { it > until } ?: break

            // A message should never be delivered out of order in this single-threaded implementation. Assert for
            // sanity
            assert(delivery >= time) { "Message delivered out of order [expected=$delivery, actual=$time]" }

            time = delivery
            queue.poll()

            val actor = registry[envelope.destination] ?: continue

            // Notice that messages for unknown/terminated actors are ignored for now
            actor.isolate { it.interpretMessage(envelope.message) }
        }

        // Jump forward in time as the caller expects the system to have run until the specified instant
        // Taking the maximum value prevents the caller to jump backwards in time
        time = max(time, until)
    }

    override fun send(msg: T, after: Duration) = schedule(this, msg, after)

    override fun terminate() {
        registry[path]?.stop(null)
        registry[system]?.stop(null)
    }

    override suspend fun <U : Any> spawnSystem(behavior: Behavior<U>, name: String): ActorRef<U> {
        return registry[system]!!.spawn(behavior, name)
    }

    override fun compareTo(other: ActorRef<*>): Int = path.compareTo(other.path)

    /**
     * The identifier for the next message to be scheduled.
     */
    private var nextId: Long = 0

    /**
     * An actor as represented in the Omega engine.
     *
     * @param self The [ActorRef] to this actor.
     * @param initialBehavior The initial behavior of this actor.
     */
    private inner class Actor<T : Any>(override val self: ActorRef<T>, initialBehavior: Behavior<T>) : ActorContext<T> {
        val childActors: MutableMap<String, Actor<*>> = mutableMapOf()
        val interpreter = BehaviorInterpreter(initialBehavior)
        val watchers: MutableSet<ActorPath> = Collections.newSetFromMap(WeakHashMap<ActorPath, Boolean>())

        override val time: Instant
            get() = this@OmegaActorSystem.time

        override val children: List<ActorRef<*>>
            get() = childActors.values.map { it.self }

        override val system: ActorSystem<*>
            get() = this@OmegaActorSystem

        override val log: Logger by lazy(LazyThreadSafetyMode.NONE) { LoggerImpl(this) }

        override fun getChild(name: String): ActorRef<*>? = childActors[name]?.self

        override fun <U : Any> send(ref: ActorRef<U>, msg: U, after: Duration) = schedule(ref, msg, after)

        override fun <U : Any> spawn(behavior: Behavior<U>, name: String): ActorRef<U> {
            require(name.isNotEmpty()) { "Actor name may not be empty" }
            require(!name.startsWith("$")) { "Actor name may not start with $-sign" }
            return internalSpawn(behavior, name)
        }

        override fun <U : Any> spawnAnonymous(behavior: Behavior<U>): ActorRef<U> {
            val name = "$" + UUID.randomUUID()
            return internalSpawn(behavior, name)
        }

        private fun <U : Any> internalSpawn(behavior: Behavior<U>, name: String): ActorRef<U> {
            require(name !in childActors) { "Actor name $name not unique" }
            val ref = ActorRefImpl<U>(this@OmegaActorSystem, self.path.child(name))
            val actor = Actor(ref, behavior)
            registry[ref.path] = actor
            childActors[name] = actor
            schedule(ref, PreStart, .0)
            actor.start()
            return ref
        }

        override fun stop(child: ActorRef<*>) {
            when {
                // Must be a direct child of this actor
                child.path.parent == self.path -> {
                    val ref = childActors[child.path.name] ?: return
                    ref.stop(null)
                }
                self == child -> throw IllegalArgumentException(
                        "Only direct children of an actor may be stopped through the actor context, " +
                            "but you tried to stop [$self] by passing its ActorRef to the `stop` method. " +
                            "Stopping self has to be expressed as explicitly returning a Stop Behavior."
                )
                else -> throw IllegalArgumentException(
                        "Only direct children of an actor may be stopped through the actor context, " +
                            "but [$child] is not a child of [$self]. Stopping other actors has to be expressed as " +
                            "an explicit stop message that the actor accepts."
                )
            }
        }

        override fun watch(target: ActorRef<*>) {
            registry[target.path]?.watchers?.add(path)
        }

        override fun unwatch(target: ActorRef<*>) {
            registry[target.path]?.watchers?.remove(path)
        }

        // Synchronization of actors in a single-threaded simulation is trivial: all actors are consistent in virtual
        // time.
        override fun sync(target: ActorRef<*>) {}

        override fun unsync(target: ActorRef<*>) {}

        override fun isSync(target: ActorRef<*>): Boolean = true

        /**
         * Start this actor.
         */
        fun start() {
            interpreter.start(this)
        }

        /**
         * Stop this actor.
         */
        fun stop(failure: Throwable?) {
            interpreter.stop(this)
            childActors.values.forEach { it.stop(failure) }
            registry.remove(self.path)
            interpreter.interpretSignal(this, PostStop)
            val termination = Terminated(self, failure)
            watchers.forEach { schedule(it, termination, 0.0) }
        }

        /**
         * Interpret the given message send to an actor.
         */
        fun interpretMessage(msg: Any) {
            if (msg is Signal) {
                interpreter.interpretSignal(this, msg)
            } else {
                @Suppress("UNCHECKED_CAST")
                interpreter.interpretMessage(this, msg as T)
            }

            if (!interpreter.isAlive) {
                stop(null)
            }
        }

        override fun equals(other: Any?): Boolean =
            other is OmegaActorSystem<*>.Actor<*> && self.path == other.self.path

        override fun hashCode(): Int = self.path.hashCode()
    }

    /**
     * Isolate uncaught exceptions originating from actor interpreter invocations.
     */
    private inline fun <T : Any, U> Actor<T>.isolate(block: (Actor<T>) -> U): U? {
        return try {
            block(this)
        } catch (t: Throwable) {
            // Forcefully stop the actor if it crashed
            stop(t)
            log.error("Unhandled exception in actor $path", t)
            null
        }
    }

    /**
     * Enumeration to track the state of the actor system.
     */
    private enum class ActorSystemState {
        CREATED, STARTED, TERMINATED
    }

    /**
     * Internal [ActorRef] implementation for this actor system.
     */
    private data class ActorRefImpl<T : Any>(
        private val owner: OmegaActorSystem<*>,
        override val path: ActorPath
    ) : ActorRef<T> {
        override fun toString(): String = "Actor[$path]"

        override fun compareTo(other: ActorRef<*>): Int = path.compareTo(other.path)
    }

    /**
     * A wrapper around a message that has been scheduled for processing.
     *
     * @property id The identifier of the message to keep the priority queue stable.
     * @property destination The destination of the message.
     * @property time The point in time to deliver the message.
     * @property message The message to wrap.
     */
    private class EnvelopeImpl(
        val id: Long,
        val destination: ActorPath,
        override val time: Instant,
        override val message: Any
    ) : Envelope<Any>

    /**
     * Schedule a message to be processed by the engine.
     *
     * @param destination The destination of the message.
     * @param message The message to schedule.
     * @param delay The time to wait before processing the message.
     */
    private fun schedule(destination: ActorRef<*>, message: Any, delay: Duration) {
        schedule(destination.path, message, delay)
    }

    /**
     * Schedule a message to be processed by the engine.
     *
     * @param path The path to the destination of the message.
     * @param message The message to schedule.
     * @param delay The time to wait before processing the message.
     */
    private fun schedule(path: ActorPath, message: Any, delay: Duration) {
        require(delay >= .0) { "The given delay must be a non-negative number" }
        queue.add(EnvelopeImpl(nextId++, path, time + delay, message))
    }
}
