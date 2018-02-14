package com.atlarge.opendc.simulator.instrumentation

import com.atlarge.opendc.simulator.kernel.Kernel
import com.atlarge.opendc.simulator.Entity
import com.atlarge.opendc.simulator.Context
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel

/**
 * A kernel instrumentation device that allows the observation and measurement of properties of interest within some
 * model.
 *
 * An instrument is a [Process] that emits measurements from within some model in the form of a typed stream. An
 * instrument is attached to a simulation using the [Kernel.install] method, which returns a [ReceiveChannel] from which
 * the measurements can be extracted out of the simulation.
 *
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
typealias Instrument<T, M> = suspend InstrumentScope<T, M>.() -> Unit

/**
 * This interface defines the scope in which an instrumentation device is built.
 *
 * An instrument is a [Process] without any observable state that is allowed to send messages to other [Entity]
 * instances in the simulation. In addition, the instrument can emit measurements using the methods provided by the
 * [SendChannel] interface.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface InstrumentScope<in T, M>: SendChannel<T>, Context<Unit, M>
