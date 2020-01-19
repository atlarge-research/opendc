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

import com.atlarge.odcsim.SimulationEngine
import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.Channel
import com.atlarge.odcsim.ProcessContext
import com.atlarge.odcsim.ProcessRef
import com.atlarge.odcsim.ReceivePort
import com.atlarge.odcsim.ReceiveRef
import com.atlarge.odcsim.SendPort
import com.atlarge.odcsim.SendRef
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.channels.Channel as KChannel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.SelectClause1
import org.jetbrains.annotations.Async
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.PriorityQueue
import java.util.UUID
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.startCoroutine

/**
 * The reference implementation of the [SimulationEngine] instance for the OpenDC simulation core.
 *
 * This engine implementation is a single-threaded implementation, running logical processes synchronously and
 * provides a single priority queue for all events (messages, ticks, etc) that occur.
 *
 * @param rootBehavior The behavior of the root actor.
 * @param name The name of the engine instance.
 */
class OmegaSimulationEngine(rootBehavior: Behavior, override val name: String) : SimulationEngine {
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
    private val queue: PriorityQueue<Event> = PriorityQueue()

    /**
     * The active processes in the simulation engine.
     */
    private val registry: MutableMap<ProcessRef, ProcessImpl> = HashMap()

    /**
     * The channels that have been registered by this engine.
     */
    private val channels: MutableSet<ChannelImpl<*>> = HashSet()

    /**
     * The [CoroutineDispatcher] instance for dispatching the coroutines representing the logical behavior.
     */
    @InternalCoroutinesApi
    private val dispatcher: CoroutineDispatcher = object : CoroutineDispatcher(), Delay {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            schedule(Event.Dispatch(clock.time, block))
        }

        override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
            schedule(Event.Resume(clock.time + timeMillis, this, continuation))
        }

        override fun invokeOnTimeout(timeMillis: Long, block: Runnable): DisposableHandle {
            val event = Event.Timeout(clock.time + timeMillis, block)
            schedule(event)
            return event
        }
    }

    init {
        spawn(rootBehavior, "/")
    }

    override suspend fun run() {
        check(state != SimulationEngineState.TERMINATED) { "The simulation engine is terminated" }

        if (state == SimulationEngineState.CREATED) {
            state = SimulationEngineState.STARTED
        }

        while (coroutineContext.isActive) {
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
    private fun process(@Async.Execute event: Event) {
        event.run()
    }

    /**
     * Spawn a new logical process in the simulation universe.
     */
    private fun spawn(behavior: Behavior, name: String): ProcessRef {
        val ref = ProcessRefImpl(this, name)
        require(ref !in registry) { "Process name $name not unique" }
        val lp = ProcessImpl(ref, behavior)
        registry[ref] = lp
        lp.start()
        return ref
    }

    /**
     * Register a new communication channel.
     */
    private fun <T : Any> open(): Channel<T> {
        val channel = ChannelImpl<T>()
        channels += channel
        return channel
    }

    private inner class ProcessImpl(override val self: ProcessRef, private val behavior: Behavior) : ProcessContext, Continuation<Unit> {
        override val clock: Clock
            get() = this@OmegaSimulationEngine.clock

        override fun spawn(behavior: Behavior): ProcessRef {
            val name = "$" + UUID.randomUUID()
            return spawn(behavior, name)
        }

        override fun spawn(behavior: Behavior, name: String): ProcessRef {
            require(name.isNotEmpty()) { "Process name may not be empty" }
            require(!name.startsWith("$")) { "Process name may not start with $-sign" }
            return this@OmegaSimulationEngine.spawn(behavior, name)
        }

        override fun <T : Any> open(): Channel<T> = this@OmegaSimulationEngine.open()

        override suspend fun <T : Any> connect(send: SendRef<T>): SendPort<T> {
            require(send is ChannelImpl && send in channels) { "Invalid reference to channel" }
            return SendPortImpl(send)
        }

        override suspend fun <T : Any> listen(receive: ReceiveRef<T>): ReceivePort<T> {
            require(receive is ChannelImpl && receive in channels) { "Invalid reference to channel" }
            return ReceivePortImpl(receive)
        }

        /**
         * Start this logical process.
         */
        fun start() = behavior.startCoroutine(this, this)

        override fun resumeWith(result: Result<Unit>) {
            // Stop the logical process
            if (result.isFailure) {
                result.exceptionOrNull()!!.printStackTrace()
            }
        }

        override val key: CoroutineContext.Key<*> = ProcessContext.Key

        @InternalCoroutinesApi
        override val context: CoroutineContext = this + dispatcher
    }

    /**
     * Enumeration to track the state of the actor system.
     */
    private enum class SimulationEngineState {
        CREATED, STARTED, TERMINATED
    }

    /**
     * Internal [ProcessRef] implementation for this simulation engine.
     */
    private data class ProcessRefImpl(
        private val owner: OmegaSimulationEngine,
        override val name: String
    ) : ProcessRef {
        override fun toString(): String = "Process[$name]"
    }

    /**
     * Internal [Channel] implementation.
     */
    private inner class ChannelImpl<T : Any> : Channel<T>, SendRef<T>, ReceiveRef<T> {
        override val send: SendRef<T> = this
        override val receive: ReceiveRef<T> = this

        /**
         * The underlying `kotlinx.coroutines` channel to back this channel implementation.
         */
        private val channel = KChannel<T>(KChannel.CONFLATED)

        val onReceive: SelectClause1<T>
            get() = channel.onReceive

        /**
         * Receive a message from this channel.
         */
        suspend fun receive(): T = channel.receive()

        /**
         * Send a message to this channel.
         */
        fun send(message: T) = assert(channel.offer(message)) { "Failed to send message" }
    }

    private inner class SendPortImpl<T : Any>(private val channelImpl: ChannelImpl<T>) : SendPort<T> {
        private var closed = false

        override fun close(): Boolean {
            if (closed) {
                return false
            }

            closed = true
            return true
        }

        override fun send(message: T) {
            check(!closed) { "Port is closed" }
            schedule(Event.Send(clock.time, channelImpl, message))
        }

    }

    private class ReceivePortImpl<T : Any>(private val channel: ChannelImpl<T>) : ReceivePort<T> {
        private var closed = false

        override fun close(): Boolean {
            if (closed) {
                return false
            }

            closed = true
            return true
        }

        override val onReceive: SelectClause1<T>
            get() = channel.onReceive

        override suspend fun receive(): T {
            check(!closed) { "Port is closed" }
            return channel.receive()
        }
    }

    /**
     * A wrapper around a message that has been scheduled for processing.
     *
     * @property time The point in time to deliver the message.
     */
    private sealed class Event(val time: Long) : Comparable<Event>, Runnable {
        override fun compareTo(other: Event): Int = time.compareTo(other.time)

        class Dispatch(time: Long, val block: Runnable) : Event(time) {
            override fun run() = block.run()

            override fun toString(): String = "Dispatch[$time]"
        }

        class Resume(time: Long, val dispatcher: CoroutineDispatcher, val continuation: CancellableContinuation<Unit>) : Event(time) {
            @InternalCoroutinesApi
            override fun run() {
                with(continuation) { dispatcher.resumeUndispatched(Unit) }
            }

            override fun toString(): String = "Resume[$time]"
        }

        class Timeout(time: Long, val block: Runnable, var cancelled: Boolean = false) : Event(time), DisposableHandle {
            override fun run() {
                if (!cancelled) {
                    block.run()
                }
            }

            override fun dispose() {
                cancelled = true
            }

            override fun toString(): String = "Dispatch[$time]"
        }

        class Send<T : Any>(time: Long, val channel: ChannelImpl<T>, val message: T) : Event(time) {
            override fun run() {
                channel.send(message)
            }
        }
    }

    /**
     * A virtual [Clock] implementation for keeping track of simulation time.
     */
    private data class VirtualClock(var time: Long) : Clock() {
        override fun withZone(zone: ZoneId?): Clock = TODO("not implemented")

        override fun getZone(): ZoneId = ZoneId.systemDefault()

        override fun instant(): Instant = Instant.ofEpochMilli(time)

        override fun millis(): Long = time
    }
}
