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

package nl.atlarge.opendc.simulator.omega

import mu.KotlinLogging
import nl.atlarge.opendc.simulator.*
import nl.atlarge.opendc.simulator.clock.Clock
import nl.atlarge.opendc.simulator.clock.Tick
import nl.atlarge.opendc.simulator.messaging.Envelope
import nl.atlarge.opendc.topology.*
import java.util.*
import kotlin.coroutines.experimental.*

/**
 * The Omega simulator is the default [Simulator] implementation for the OpenDC core.
 *
 * <p>This simulator implementation is a single-threaded implementation running simulation kernels synchronously and
 * provides a single priority queue for all events (messages, ticks, etc) that occur in the components.
 *
 * <p>By default, [Kernel]s are resolved as part of the [Topology], meaning each [Component] in the topology also
 * implements its simulation logic by deriving from the [Kernel] interface.
 *
 * @param topology The topology to run the simulation over.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
class OmegaSimulator(override val topology: Topology) : Simulator, Iterator<Unit> {
	/**
	 * The logger instance to use for the simulator.
	 */
	private val logger = KotlinLogging.logger {}

	/**
	 * The registry of the simulation kernels used in the experiment.
	 */
	private val registry: MutableMap<Component<*>, Context<*>?> = HashMap()

	/**
	 * A mapping of the entities in the topology to their current state.
	 */
	private val states: MutableMap<Entity<*>, Any?> = HashMap()

	/**
	 * The clock of the simulator.
	 */
	private val clock: OmegaClock = OmegaClock()

	/**
	 * Initialize the simulator.
	 */
	init {
		topology.forEach { node ->
			resolve(node)
			node.outgoingEdges.forEach { resolve(it) }
		}

		registry.values.forEach { context ->
			if (context == null)
				return@forEach
			@Suppress("UNCHECKED_CAST")
			val kernel = context.component.label as Kernel<Context<*>>

			// Start all kernel co-routines
			val block: suspend () -> Unit = { kernel.run { context.simulate() } }
			block.startCoroutine(KernelCoroutine())
		}
	}

	/**
	 * Resolve the given [Component] to the [Kernel] of that component.
	 *
	 * @param component The component to resolve.
	 * @return The [Kernel] that simulates that [Component].
	 */
	fun <T : Component<*>> resolve(component: T): Context<T>? {
		@Suppress("UNCHECKED_CAST")
		return registry.computeIfAbsent(component, {
			if (component.label !is Kernel<*>)
				null
			else when (component) {
				is Node<*> -> OmegaEntityContext(component as Node<*>)
				is Edge<*> -> OmegaChannelContext(component as Edge<*>)
				else -> null
			}
		}) as Context<T>?
	}

	/**
	 * Determine whether the simulator has a next non-empty cycle available.
	 *
	 * @return <code>true</code> if the simulator has a next non-empty cycle, <code>false</code> otherwise.
	 */
	override fun hasNext(): Boolean = clock.queue.isNotEmpty()

	/**
	 * Run the next cycle in the simulation.
	 */
	override fun next() {
		clock.tick++
		while (true) {
			val (tick, block) = clock.queue.peek() ?: return

			if (tick > clock.tick) {
				// Tick has yet to occur
				// Jump in time to next event
				clock.tick = tick - 1
				break
			} else if (tick < clock.tick) {
				// Tick has already occurred
				logger.warn { "tick was not handled correctly" }
			}
			clock.queue.poll()
			block()
		}
	}

	/**
	 * The co-routine which runs a simulation kernel.
	 */
	private class KernelCoroutine : Continuation<Unit> {
		override val context: CoroutineContext = EmptyCoroutineContext
		override fun resume(value: Unit) {}

		override fun resumeWithException(exception: Throwable) {
			val currentThread = Thread.currentThread()
			currentThread.uncaughtExceptionHandler.uncaughtException(currentThread, exception)
		}
	}

	/**
	 * The [Clock] for this [OmegaSimulator] that keeps track of the simulation time in ticks.
	 */
	private inner class OmegaClock : Clock {
		override var tick: Tick = 0
		internal val queue: PriorityQueue<Pair<Tick, () -> Unit>> = PriorityQueue(Comparator.comparingLong { it.first })

		override fun scheduleAt(tick: Tick, block: () -> Unit) {
			queue.add(Pair(tick, block))
		}
	}

	/**
	 * This internal class provides the default implementation for the [Context] interface for this simulator.
	 */
	private abstract inner class OmegaAbstractContext<out T : Component<*>> : Context<T> {
		/**
		 * The [Topology] over which the simulation is run.
		 */
		override val topology: Topology = this@OmegaSimulator.topology

		/**
		 * Retrieves and removes a single message from this channel suspending the caller while the channel is empty.
		 *
		 * @param block The block to process the message with.
		 * @return The processed message.
		 */
		suspend override fun <T> receive(block: Envelope<*>.(Any?) -> T): T {
			TODO("not implemented")
		}

		/**
		 * The observable state of an [Entity] within the simulation is provided by the context of the simulation.
		 */
		@Suppress("UNCHECKED_CAST")
		override val <S> Entity<S>.state: S
			get() = states.computeIfAbsent(this, { initialState }) as S

		/**
		 * Suspend the simulation kernel for <code>n</code> ticks before resuming the execution.
		 *
		 * @param n The amount of ticks to suspend the simulation kernel, with <code>n > 0</code>
		 */
		suspend override fun wait(n: Int) {
			require(n > 0) { "The amount of ticks to suspend must be a non-zero positive number" }
			return suspendCoroutine { cont ->
				clock.scheduleAfter(n, { cont.resume(Unit) })
			}
		}
	}

	/**
	 * An internal class to provide [Context] for an entity within the simulation.
	 */
	private inner class OmegaEntityContext<out T : Entity<*>>(override val component: Node<T>) : OmegaAbstractContext<Node<T>>(), EntityContext<T> {
		/**
		 * Update the state of the entity being simulated.
		 *
		 * <p>Instead of directly mutating the entity, we create a new instance of the entity to prevent other objects
		 * referencing the old entity having their data changed.
		 *
		 * @param next The next state of the entity.
		 */
		suspend override fun <C : EntityContext<E>, E : Entity<S>, S> C.update(next: S) {
			states.put(component.entity as Entity<*>, next)
		}
	}

	/**
	 * An internal class to provide the [Context] for an edge kernel within the simulation.
	 */
	private inner class OmegaChannelContext<out T>(override val component: Edge<T>) : OmegaAbstractContext<Edge<T>>(), ChannelContext<T> {
		/**
		 * Send the given message downstream.
		 *
		 * @param msg The message to send.
		 * @param sender The sender of the message.
		 */
		suspend override fun send(msg: Any?, sender: Node<*>) {
			TODO("not implemented")
		}
	}
}
