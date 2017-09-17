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
 * @param topology The topology to run the simulation over.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
class OmegaKernel(override val topology: Topology) : Kernel {
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
	private val queue: PriorityQueue<Envelope<*>> = PriorityQueue(Comparator.comparingLong(Envelope<*>::tick))

	/**
	 * The clock of the simulator.
	 */
	private val clock: OmegaClock = OmegaClock()

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
			val tick = envelope.tick

			if (tick > clock.tick) {
				// Tick has yet to occur
				// Jump in time to next event
				clock.tick = tick
				break
			} else if (tick < clock.tick) {
				// Tick has already occurred
				logger.warn { "message processed out of order" }
			}
			queue.poll()

			val context = registry[envelope.destination] ?: continue

			if (envelope.message !is Interrupt) {
				context.continuation.resume(envelope)
			} else {
				context.continuation.resumeWithException(envelope.message as Interrupt)
			}
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
	 * Schedule a message for processing by a [Process].
	 *
	 * @param message The message to schedule.
	 * @param destination The destination of the message.
	 * @param sender The sender of the message.
	 * @param delay The amount of ticks to wait before processing the message.
	 */
	override fun schedule(message: Any?, destination: Entity<*>, sender: Entity<*>?, delay: Int) {
		require(delay > 0) { "The amount of ticks to delay the message must be a positive number" }
		queue.add(Envelope(message, clock.tick + delay, sender, destination))
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
		 * The state of the entity.
		 */
		var state: S = entity.initialState

		/**
		 * The [Topology] over which the simulation is run.
		 */
		override val topology: Topology = this@OmegaKernel.topology

		/**
		 * The global [Clock] that keeps track of the simulation time.
		 */
		override val clock: Clock = this@OmegaKernel.clock

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
		 * Retrieves and removes a single message from this channel suspending the caller while the channel is empty.
		 *
		 * @param block The block to process the message with.
		 * @return The processed message.
		 */
		suspend override fun <T> receive(block: Envelope<*>.(Any?) -> T): T {
			val envelope = receiveEnvelope()
			return block(envelope, envelope.message)
		}

		/**
		 * Send the given message downstream.
		 *
		 * @param msg The message to send.
		 * @param sender The sender of the message.
		 * @param delay The number of ticks before the message should be received.
		 */
		suspend override fun Entity<*>.send(msg: Any?, sender: Entity<*>?, delay: Int) {
			schedule(msg, this, sender, delay)
		}

		/**
		 * Send an interruption message to the given [Entity].
		 */
		suspend override fun Entity<*>.interrupt() = send(Interrupt, this)

		/**
		 * Suspend the simulation kernel until the next tick occurs in the simulation.
		 */
		suspend override fun tick(): Boolean {
			wait(1)
			return true
		}

		/**
		 * Suspend the simulation kernel for <code>n</code> ticks before resuming the execution.
		 *
		 * @param n The amount of ticks to suspend the simulation kernel, with <code>n > 0</code>
		 */
		suspend override fun wait(n: Int) {
			require(n > 0) { "The amount of ticks to suspend must be a non-zero positive number" }
			queue.add(Envelope(Resume, clock.tick + n, entity, entity))

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
