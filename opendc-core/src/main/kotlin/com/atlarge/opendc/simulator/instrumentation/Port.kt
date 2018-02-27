package com.atlarge.opendc.simulator.instrumentation

import com.atlarge.opendc.simulator.kernel.Simulation
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel

/**
 * A port allows users to install instrumentation devices to a [Simulation].
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Port<M> {
    /**
     * Install the given instrumentation device to produce a stream of measurements of type <code>T</code>.
     *
     * The [ReceiveChannel] returned by this channel is by default unlimited, which means the channel buffers at most
     * one measurement, so that the receiver always gets the most recently sent element.
     * Back-to-send sent measurements are conflated – only the the most recently sent element is received, while
     * previously sent elements are lost.
     *
     * @param instrument The instrumentation device to install.
     * @return A [ReceiveChannel] to which the of measurements produced by the instrument are published.
     */
    fun <T> install(instrument: Instrument<T, M>): ReceiveChannel<T> = install(Channel.CONFLATED, instrument)

    /**
     * Install the given instrumentation device to produce a stream of measurements of type code>T</code>.
     *
     * @param capacity The capacity of the buffer of the channel.
     * @param instrument The instrumentation device to install.
     * @return A [ReceiveChannel] to which the of measurements produced by the instrument are published.
     */
    fun <T> install(capacity: Int, instrument: Instrument<T, M>): ReceiveChannel<T>

    /**
     * Close this port and stop the instruments from producing more measurements.
     * This is an idempotent operation – repeated invocations of this function have no effect and return false.
     *
     * @param cause An optional cause that is thrown when trying to receive more elements from the installed
     * instruments.
     * @return `true` if the port was closed, `false` if it was already closed.
     */
    fun close(cause: Throwable? = null): Boolean
}
