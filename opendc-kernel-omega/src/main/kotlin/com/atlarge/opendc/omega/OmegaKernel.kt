/*
 * MIT License
 *
 * Copyright (c) 2017 atlarge-research
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

package com.atlarge.opendc.omega

import com.atlarge.opendc.simulator.*
import com.atlarge.opendc.simulator.kernel.Kernel
import mu.KotlinLogging
import java.util.*
import kotlin.coroutines.experimental.*

/**
 * The Omega simulation kernel is the reference simulation kernel implementation for the OpenDC Simulator core.
 *
 * This simulator implementation is a single-threaded implementation, running simulation kernels synchronously and
 * provides a single priority queue for all events (messages, ticks, etc) that occur in the entities.
 *
 * @property model The model that is simulated.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
internal class OmegaKernel<M>(bootstrap: Bootstrap<M>) : Kernel<M>, Bootstrap.Context<M> {
    /**
     * The logger instance to use for the simulator.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The registry of the simulation kernels used in the experiment.
     */
    private val registry: MutableMap<Entity<*, *>, OmegaContext<*>> = HashMap()

    /**
     * The message queue.
     */
    private val queue: Queue<MessageContainer> = PriorityQueue(Comparator.comparingLong(MessageContainer::time))

    /**
     * The processes to be spawned.
     */
    private val spawnings: Queue<Process<*, M>> = ArrayDeque()

    /**
     * The simulation time.
     */
    override var time: Instant = 0

    /**
     * The model of simulation.
     */
    override val model: M = bootstrap.bootstrap(this)

    override val <E : Entity<S, *>, S> E.state: S
        get() = context?.state ?: initialState

    /**
     * The context associated with an [Entity].
     */
    @Suppress("UNCHECKED_CAST")
    private val <E : Entity<S, M>, S, M> E.context: OmegaContext<S>?
        get() = registry[this] as? OmegaContext<S>

    override fun register(entity: Entity<*, M>): Boolean {
        if (!registry.containsKey(entity) && entity !is Process) {
            return false
        }

        val process = entity as Process<*, M>
        spawnings.add(process)
        return true
    }

    override fun deregister(entity: Entity<*, M>): Boolean {
        val context = entity.context ?: return false
        context.resume(Unit)
        return true
    }

    override fun schedule(message: Any, destination: Entity<*, *>, sender: Entity<*, *>?, delay: Duration) =
        schedule(prepare(message, destination, sender, delay))

    override fun step() {
        while (true) {
            // Initialise all spawned processes
            while (spawnings.isNotEmpty()) {
                val process = spawnings.poll()
                val context = OmegaContext(process).also { registry[process] = it }

                // Bootstrap the process coroutine
                val block: suspend () -> Unit = { context.start() }
                block.startCoroutine(context)

            }

            val envelope = queue.peek() ?: return
            val delivery = envelope.time

            if (delivery > time) {
                // Tick has yet to occur
                // Jump in time to next event
                time = delivery
                break
            } else if (delivery < time) {
                // Tick has already occurred
                logger.warn { "message processed out of order" }
            }

            queue.poll()

            // If the sender has canceled the message, we move on to the next message
            if (envelope.canceled) {
                continue
            }

            val context = envelope.destination.context ?: continue

            if (envelope.message !is Interrupt) {
                context.continuation.resume(envelope)
            } else {
                context.continuation.resumeWithException(envelope.message)
            }

            context.last = time
        }
    }


    override fun run() {
        while (queue.isNotEmpty() || spawnings.isNotEmpty()) {
            step()
        }
    }

    override fun run(until: Instant) {
        require(until > 0) { "The given instant must be a non-zero positive number" }

        if (time >= until) {
            return
        }

        while (time < until && (queue.isNotEmpty() || spawnings.isNotEmpty())) {
            step()
        }

        // Fix clock if step() jumped too far in time to give the impression to the user that simulation stopped at
        // exactly the tick it gave. This has not effect on the actual simulation results as the next call to run() will
        // just jump forward again.
        if (time > until) {
            time = until
        }
    }

    private fun schedule(envelope: MessageContainer) {
        queue.add(envelope)
    }

    private fun prepare(message: Any, destination: Entity<*, *>, sender: Entity<*, *>? = null,
                        delay: Duration): MessageContainer {
        require(delay >= 0) { "The amount of time to delay the message must be a positive number" }
        return MessageContainer(message, time + delay, sender, destination)
    }

    /**
     * This internal class provides the default implementation for the [Context] interface for this simulator.
     */
    private inner class OmegaContext<S>(val process: Process<S, M>) : Context<S, M>, Continuation<Unit> {
        /**
         * The continuation to resume the execution of the process.
         */
        lateinit var continuation: Continuation<Envelope<*>>

        /**
         * The last point in time the process has done some work.
         */
        var last: Instant = -1

        /**
         * The model in which the process exists.
         */
        override val model: M
            get() = this@OmegaKernel.model

        /**
         * The state of the entity.
         */
        override var state: S = process.initialState

        /**
         * The current point in simulation time.
         */
        override val time: Instant
            get() = this@OmegaKernel.time

        /**
         * The duration between the current point in simulation time and the last point in simulation time where the
         * [Context] has executed some work.
         */
        override val delta: Duration
            get() = maxOf(time - last, 0)

        /**
         * The [CoroutineContext] for a [Context].
         */
        override val context: CoroutineContext = EmptyCoroutineContext

        /**
         * The observable state of an [Entity] within the simulation is provided by the context of the simulation.
         */
        override val <T : Entity<S, *>, S> T.state: S
            get() = context?.state ?: initialState

        /**
         * Start the process associated with this context.
         */
        internal suspend fun start() = process.run {
            run()
        }

        /**
         * Retrieve and remove and single message from the mailbox of the [Entity] and suspend the [Context] until the
         * message has been received.
         *
         * @return The envelope containing the message.
         */
        suspend fun receiveEnvelope(): Envelope<*> = suspendCoroutine { continuation = it }

        override suspend fun <T> receive(transform: suspend Envelope<*>.(Any) -> T): T {
            val envelope = receiveEnvelope()
            return transform(envelope, envelope.message)
        }


        override suspend fun <T> receive(timeout: Duration, transform: suspend Envelope<*>.(Any) -> T): T? {
            val send = prepare(Timeout, process, process, timeout).also { schedule(it) }

            try {
                val received = receiveEnvelope()

                if (received.message == Timeout) {
                    send.canceled = true
                    return transform(received, received.message)
                }

                return null
            } finally {
                send.canceled = true
            }
        }

        override suspend fun Entity<*, *>.send(msg: Any, delay: Duration) = send(msg, process, delay)

        override suspend fun Entity<*, *>.send(msg: Any, sender: Entity<*, *>, delay: Duration) =
            schedule(prepare(msg, sender, delay = delay))

        override suspend fun Entity<*, *>.interrupt() = send(Interrupt)

        override suspend fun hold(duration: Duration) {
            require(duration >= 0) { "The amount of time to hold must be a positive number" }
            val envelope = prepare(Resume, process, process, duration).also { schedule(it) }

            try {
                while (true) {
                    if (receive() == Resume)
                        return
                }
            } finally {
                envelope.canceled = true
            }
        }

        override suspend fun hold(duration: Duration, queue: Queue<Any>) {
            require(duration >= 0) { "The amount of time to hold must be a positive number" }
            val envelope = prepare(Resume, process, process, duration).also { schedule(it) }

            try {
                while (true) {
                    val msg = receive()
                    if (msg == Resume)
                        return
                    queue.add(msg)
                }
            } finally {
                envelope.canceled = true
            }
        }


        // Completion continuation implementation
        /**
         * Resume the execution of this continuation with the given value.
         *
         * @param value The value to resume with.
         */
        override fun resume(value: Unit) {
            // Deregister process from registry in order to have the GC collect this context
            registry.remove(process)
        }

        /**
         * Resume the execution of this continuation with an exception.
         *
         * @param exception The exception to resume with.
         */
        override fun resumeWithException(exception: Throwable) {
            // Deregister process from registry in order to have the GC collect this context:w
            registry.remove(process)

            logger.error(exception) { "An exception occurred during the execution of a process" }
        }
    }
}
