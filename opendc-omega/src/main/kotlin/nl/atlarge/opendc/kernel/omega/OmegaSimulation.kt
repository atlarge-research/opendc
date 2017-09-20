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

package nl.atlarge.opendc.kernel.omega

import mu.KotlinLogging
import nl.atlarge.opendc.kernel.*
import nl.atlarge.opendc.kernel.messaging.Envelope
import nl.atlarge.opendc.kernel.messaging.Receipt
import nl.atlarge.opendc.kernel.time.Clock
import nl.atlarge.opendc.kernel.time.Duration
import nl.atlarge.opendc.kernel.time.Instant
import nl.atlarge.opendc.kernel.time.TickClock
import nl.atlarge.opendc.topology.Entity
import nl.atlarge.opendc.topology.Topology
import nl.atlarge.opendc.topology.TopologyContext
import java.util.*
import kotlin.coroutines.experimental.*

/**
 * The Omega simulation kernel is the reference simulation kernel implementation for the OpenDC Simulator core.
 *
 * This simulator implementation is a single-threaded implementation, running simulation kernels synchronously and
 * provides a single priority queue for all events (messages, ticks, etc) that occur in the entities.
 *
 * By default, [Process]s are resolved as part of the [Topology], meaning each [Entity] in the topology should also
 * implement its simulation behaviour by deriving from the [Process] interface.
 *
 * @property kernel The kernel that facilitates the simulation.
 * @property topology The topology to run the simulation over.
 * @property clock The clock to use for simulation time.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
internal class OmegaSimulation(override val kernel: OmegaKernel, override val topology: Topology,
							   override val clock: Clock = TickClock()) : Simulation {
	/**
	 * The logger instance to use for the simulator.
	 */
	private val logger = KotlinLogging.logger {}

	/**
	 * The registry of the simulation kernels used in the experiment.
	 */
	private val registry: MutableMap<Entity<*>, OmegaContext<*, *>> = HashMap()

	/**
	 * The message queue.
	 */
	private val queue: Queue<MessageContainer> = PriorityQueue(Comparator.comparingLong(MessageContainer::time))

	/**
	 * Initialise the simulator.
	 */
	init {
		topology.forEach { resolve(it) }
		registry.values.forEach { context ->
			@Suppress("UNCHECKED_CAST")
			val process = context.entity as Process<Entity<*>>

			// Start all process co-routines
			val block: suspend () -> Unit = { process.run { context.run() } }
			block.startCoroutine(context)
		}
	}

	/**
	 * Step through one event in the simulation.
	 */
	override fun step() {
		while (true) {
			val envelope = queue.peek() ?: return
			val delivery = envelope.time

			if (delivery > clock.now) {
				// Tick has yet to occur
				// Jump in time to next event
				clock.advanceTo(delivery)
				break
			} else if (delivery < clock.now) {
				// Tick has already occurred
				logger.warn { "message processed out of order" }
			}
			// Remove the message from the queue
			queue.poll()

			// If the sender has canceled the message, we move on to the next message
			if (envelope.canceled) {
				continue
			}

			val context = registry[envelope.destination] ?: continue

			if (envelope.message !is Interrupt) {
				context.continuation.resume(envelope)
			} else {
				context.continuation.resumeWithException(envelope.message)
			}

			context.last = clock.now
		}
	}

	/**
	 * Run a simulation over the specified [Topology].
	 * This method will step through multiple cycles in the simulation until no more message exist in the queue.
	 */
	override fun run() {
		while (queue.isNotEmpty()) {
			step()
		}
	}

	/**
	 * Run a simulation over the specified [Topology], stepping through cycles until (exclusive) the specified clock
	 * tick has occurred. The control is then handed back to the user.
	 *
	 * @param until The point in simulation time at which the simulation should be paused and the control is handed
	 * 				back to the user.
	 */
	override fun run(until: Instant) {
		require(until > 0) { "The given instant must be a non-zero positive number" }

		if (clock.now >= until) {
			return
		}

		while (clock.now < until && queue.isNotEmpty()) {
			step()
		}

		// Fix clock if step() jumped too far in time to give the impression to the user that simulation stopped at
		// exactly the tick it gave. This has not effect on the actual simulation results as the next call to run() will
		// just jump forward again.
		if (clock.now > until) {
			clock.rewindTo(until)
		}
	}

	/**
	 * Schedule a message for processing by a [Process].
	 *
	 * @param message The message to schedule.
	 * @param destination The destination of the message.
	 * @param sender The sender of the message.
	 * @param delay The amount of time to wait before processing the message.
	 */
	override fun schedule(message: Any, destination: Entity<*>, sender: Entity<*>?, delay: Duration): Receipt {
		require(delay >= 0) { "The amount of time to delay the message must be a positive number" }
		val wrapped = MessageContainer(message, clock.now + delay, sender, destination)
		queue.add(wrapped)
		return wrapped
	}

	/**
	 * Resolve the given [Context], given an [Entity] in a logical topology of a cloud network.
	 *
	 * @param entity The [Entity] to resolve the [Context] for.
	 * @return The [Context] for the given [Entity] or <code>null</code> if the component has no [Process] associated
	 * with it.
	 */
	private fun <E : Entity<*>> resolve(entity: E): Context<E>? {
		if (entity !is Process<*>)
			return null

		@Suppress("UNCHECKED_CAST")
		return registry.computeIfAbsent(entity, {
			OmegaContext(entity)
		}) as Context<E>
	}

	/**
	 * This internal class provides the default implementation for the [Context] interface for this simulator.
	 */
	private inner class OmegaContext<out E : Entity<S>, S>(override val entity: E) : Context<E>,
		Continuation<Unit>, TopologyContext by topology {
		/**
		 * The continuation to resume the execution of the process.
		 */
		lateinit var continuation: Continuation<Envelope<*>>

		/**
		 * The last point in time the process has done some work.
		 */
		var last: Instant = 0

		/**
		 * The state of the entity.
		 */
		var state: S = entity.initialState

		/**
		 * The [Topology] over which the simulation is run.
		 */
		override val topology: Topology = this@OmegaSimulation.topology

		/**
		 * The current point in simulation time.
		 */
		override val time: Instant
			get() = clock.now

		/**
		 * The duration between the current point in simulation time and the last point in simulation time where the
		 * [Process] has executed some work.
		 */
		override val delta: Duration
			get() = clock.now - last

		/**
		 * The [CoroutineContext] for a [Process].
		 */
		override val context: CoroutineContext = EmptyCoroutineContext

		/**
		 * The observable state of an [Entity] within the simulation is provided by the context of the simulation.
		 */
		@Suppress("UNCHECKED_CAST")
		override val <T : Entity<S>, S> T.state: S
			get() = (resolve(this) as OmegaContext<T, S>?)?.state ?: initialState

		/**
		 * Retrieve and remove and single message from the mailbox of the [Entity] and suspend the [Process] until the
		 * message has been received.
		 *
		 * @return The envelope containing the message.
		 */
		suspend fun receiveEnvelope(): Envelope<*> {
			return suspendCoroutine { continuation = it }
		}

		/**
		 * Retrieve and removes a single message from the entity's mailbox, suspending the function if the mailbox is empty.
		 * The execution is resumed after the message has landed in the entity's mailbox after which the message [Envelope]
		 * is mapped through `block` to generate a processed message.
		 *
		 * @param block The block to process the message with.
		 * @return The processed message.
		 */
		suspend override fun <T> receive(block: suspend Envelope<*>.(Any) -> T): T {
			val envelope = receiveEnvelope()
			return block(envelope, envelope.message)
		}

		/**
		 * Retrieve and removes a single message from the entity's mailbox, suspending the function if the mailbox is empty.
		 * The execution is resumed after the message has landed in the entity's mailbox or the timeout was reached,
		 *
		 * If the message has been received, the message [Envelope] is mapped through `block` to generate a processed
		 * message. If the timeout was reached, `block` is not called and `null` is returned.
		 *
		 * @param timeout The duration to wait before resuming execution.
		 * @param block The block to process the message with.
		 * @return The processed message or `null` if the timeout was reached.
		 */
		suspend override fun <T> receive(timeout: Duration, block: suspend Envelope<*>.(Any) -> T): T? {
			val receipt = schedule(Timeout, entity, entity, timeout)
			val envelope = receiveEnvelope()

			if (envelope.message !is Timeout) {
				receipt.cancel()
				return block(envelope, envelope.message)
			}

			return null
		}

		/**
		 * Send the given message to the specified entity.
		 *
		 * @param msg The message to send.
		 * @param delay The amount of time to wait before the message should be received.
		 */
		suspend override fun Entity<*>.send(msg: Any, delay: Duration) = send(msg, entity, delay)

		/**
		 * Send the given message to the specified entity.
		 *
		 * @param msg The message to send.
		 * @param sender The sender of the message.
		 * @param delay The amount of time to wait before the message should be received.
		 */
		suspend override fun Entity<*>.send(msg: Any, sender: Entity<*>, delay: Duration): Receipt {
			return schedule(msg, this, sender, delay)
		}

		/**
		 * Send an interruption message to the given [Entity].
		 */
		suspend override fun Entity<*>.interrupt() {
			send(Interrupt)
		}

		/**
		 * Suspend the [Process] of the [Entity] in simulation for one tick in simulation time which is defined by the
		 * [Clock].
		 *
		 * @return `true` to allow usage in while statements.
		 */
		suspend override fun tick(): Boolean {
			wait(clock.tick)
			return true
		}

		/**
		 * Suspend the [Process] of the [Entity] in simulation for the given duration of simulation time before resuming
		 * execution.
		 *
		 * A call to this method will not make the [Process] sleep for the actual duration of time, but instead suspend
		 * the process until the no more messages at an earlier point in time have to be processed.
		 *
		 * @param duration The duration of simulation time to wait before resuming execution.
		 */
		suspend override fun wait(duration: Duration) {
			require(duration > 0) { "The amount of time to suspend must be a non-zero positive number" }
			schedule(Resume, entity, entity, duration)

			while (true) {
				if (receive() is Resume)
					return
			}
		}

		/**
		 * Update the state of the entity being simulated.
		 *
		 * <p>Instead of directly mutating the entity, we create a new instance of the entity to prevent other objects
		 * referencing the old entity having their data changed.
		 *
		 * @param next The next state of the entity.
		 */
		suspend override fun <C : Context<E>, E : Entity<S>, S> C.update(next: S) {
			@Suppress("UNCHECKED_CAST")
			(this as OmegaContext<E, S>).state = next
		}

		// Completion continuation implementation
		/**
		 * Resume the execution of this continuation with the given value.
		 *
		 * @param value The value to resume with.
		 */
		override fun resume(value: Unit) {}

		/**
		 * Resume the execution of this continuation with an exception.
		 *
		 * @param exception The exception to resume with.
		 */
		override fun resumeWithException(exception: Throwable) {
			val currentThread = Thread.currentThread()
			currentThread.uncaughtExceptionHandler.uncaughtException(currentThread, exception)
		}
	}
}
