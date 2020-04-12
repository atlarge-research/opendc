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

import com.atlarge.odcsim.Domain
import com.atlarge.odcsim.SimulationContext
import com.atlarge.odcsim.SimulationEngine
import com.atlarge.odcsim.engine.omega.logging.LoggerImpl
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.PriorityQueue
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Delay
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.annotations.Async
import org.slf4j.Logger

/**
 * The reference implementation of the [SimulationEngine] instance for the OpenDC simulation core.
 *
 * This engine implementation is a single-threaded implementation, running logical processes synchronously and
 * provides a single priority queue for all events (messages, ticks, etc) that occur.
 *
 * @param name The name of the engine instance.
 */
public class OmegaSimulationEngine(override val name: String) : SimulationEngine {
    /**
     * The state of the actor system.
     */
    private var state: SimulationEngineState = SimulationEngineState.CREATED

    /**
     * The clock tracking the simulation time.
     */
    private val clock: VirtualClock = VirtualClock(0)

    /**
     * The event queue to process
     */
    private val queue: PriorityQueue<Event> = PriorityQueue(Comparator<Event> { lhs, rhs ->
        // Note that Comparator gives better performance than Comparable according to
        // profiling
        val cmp = lhs.time.compareTo(rhs.time)
        if (cmp == 0) lhs.id.compareTo(rhs.id) else cmp
    })

    /**
     * The active processes in the simulation engine.
     */
    private val registry: MutableMap<String, Domain> = HashMap()

    /**
     * A unique increasing identifier assigned to each event, needed because otherwise two events occurring in sequence
     * (but at the same time) may be differently ordered in the internal priority queue (queue) since it does not
     * guarantee insertion order.
     */
    private var nextId: Long = 0

    override fun newDomain(): Domain = newDomain(null)

    override fun newDomain(name: String): Domain = newDomain(name, null)

    override suspend fun run() {
        check(state != SimulationEngineState.TERMINATED) { "The simulation engine is terminated" }

        if (state == SimulationEngineState.CREATED) {
            state = SimulationEngineState.STARTED
        }

        val job = coroutineContext[Job]

        while (job?.isActive == true) {
            val event = queue.peek() ?: break
            val delivery = event.time

            // A message should never be delivered out of order in this single-threaded implementation. Assert for
            // sanity
            assert(delivery >= clock.time) { "Message delivered out of order [expected=$delivery, actual=${clock.time}]" }

            clock.time = delivery
            queue.poll()

            process(event)
        }
    }

    override suspend fun terminate() {
        state = SimulationEngineState.TERMINATED
    }

    /**
     * Schedule the specified event to be processed by the engine.
     */
    private fun schedule(@Async.Schedule event: Event) {
        queue.add(event)
    }

    /**
     * Process the delivery of an event.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun process(@Async.Execute event: Event) {
        // This has been inlined into this method for performance
        when (event) {
            is Event.Dispatch ->
                event.block.run()
            is Event.Resume ->
                with(event.continuation) { event.dispatcher.resumeUndispatched(Unit) }
            is Event.Timeout ->
                if (!event.isCancelled)
                    event.block.run()
        }
    }

    /**
     * Spawn a new simulation domain.
     */
    private fun newDomainImpl(name: String, parent: DomainImpl?): Domain {
        val domain = DomainImpl(name, parent)
        require(domain.path !in registry) { "Domain name $name not unique" }
        registry[domain.path] = domain
        return domain
    }

    private fun newDomain(parent: DomainImpl?): Domain {
        val name = "$" + UUID.randomUUID()
        return newDomainImpl(name, parent)
    }

    private fun newDomain(name: String, parent: DomainImpl?): Domain {
        require(name.isNotEmpty()) { "Domain name may not be empty" }
        require(!name.startsWith("$")) { "Domain name may not start with $-sign" }
        require(!name.contains("/")) { "Domain name may not contain /" }
        return newDomainImpl(name, parent)
    }

    private inner class DomainImpl(override val name: String, parent: DomainImpl?) : SimulationContext, Domain {
        val job: Job = SupervisorJob(parent?.job)
        val path: String = (parent?.path ?: "") + "/$name"

        @InternalCoroutinesApi
        private val dispatcher = object : CoroutineDispatcher(), Delay {
            // CoroutineDispatcher
            override fun dispatch(context: CoroutineContext, block: Runnable) {
                schedule(Event.Dispatch(clock.time, nextId++, block))
            }

            // Delay
            override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
                schedule(Event.Resume(clock.time + timeMillis, nextId++, this, continuation))
            }

            override fun invokeOnTimeout(timeMillis: Long, block: Runnable): DisposableHandle {
                val event = Event.Timeout(clock.time + timeMillis, nextId++, block)
                schedule(event)
                return event
            }
        }

        private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            log.error("Uncaught exception", exception)
        }

        // SimulationContext
        override val key: CoroutineContext.Key<*> = SimulationContext.Key

        override val domain: Domain = this

        override val clock: VirtualClock
            get() = this@OmegaSimulationEngine.clock

        override val log: Logger by lazy(LazyThreadSafetyMode.NONE) { LoggerImpl.invoke(this) }

        override fun newDomain(): Domain = this@OmegaSimulationEngine.newDomain(this)

        override fun newDomain(name: String): Domain = this@OmegaSimulationEngine.newDomain(name, this)

        // Domain
        override val parent: Domain = parent ?: this

        @InternalCoroutinesApi
        override val coroutineContext: CoroutineContext = this + CoroutineName(name) + dispatcher + job + exceptionHandler

        override fun toString(): String = path
    }

    /**
     * Enumeration to track the state of the actor system.
     */
    private enum class SimulationEngineState {
        CREATED, STARTED, TERMINATED
    }

    /**
     * A wrapper around a message that has been scheduled for processing.
     *
     * @property time The point in time to deliver the message.
     */
    private sealed class Event(val time: Long, val id: Long) {
        class Dispatch(time: Long, id: Long, val block: Runnable) : Event(time, id) {
            override fun toString(): String = "Dispatch[$time]"
        }

        class Resume(time: Long, id: Long, val dispatcher: CoroutineDispatcher, val continuation: CancellableContinuation<Unit>) : Event(time, id) {
            override fun toString(): String = "Resume[$time]"
        }

        class Timeout(time: Long, id: Long, val block: Runnable, var isCancelled: Boolean = false) : Event(time, id), DisposableHandle {
            override fun dispose() {
                isCancelled = true
            }

            override fun toString(): String = "Timeout[$time]"
        }
    }

    /**
     * A virtual [Clock] implementation for keeping track of simulation time.
     */
    private data class VirtualClock(var time: Long) : Clock() {
        override fun withZone(zone: ZoneId?): Clock = throw NotImplementedError()

        override fun getZone(): ZoneId = ZoneId.systemDefault()

        override fun instant(): Instant = Instant.ofEpochMilli(time)

        override fun millis(): Long = time
    }
}
