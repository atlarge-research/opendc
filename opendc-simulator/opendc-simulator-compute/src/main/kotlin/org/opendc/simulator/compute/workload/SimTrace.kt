/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.simulator.compute.workload

import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.flow.FlowConnection
import org.opendc.simulator.flow.FlowSource
import kotlin.math.min

/**
 * A workload trace that describes the resource utilization over time in a collection of [SimTraceFragment]s.
 *
 * @param usageCol The column containing the CPU usage of each fragment (in MHz).
 * @param timestampCol The column containing the starting timestamp for each fragment (in epoch millis).
 * @param deadlineCol The column containing the ending timestamp for each fragment (in epoch millis).
 * @param coresCol The column containing the utilized cores.
 * @param size The number of fragments in the trace.
 */
public class SimTrace(
    private val usageCol: DoubleArray,
    private val timestampCol: LongArray,
    private val deadlineCol: LongArray,
    private val coresCol: IntArray,
    private val size: Int,
) {
    init {
        require(size >= 0) { "Invalid trace size" }
        require(usageCol.size >= size) { "Invalid number of usage entries" }
        require(timestampCol.size >= size) { "Invalid number of timestamp entries" }
        require(deadlineCol.size >= size) { "Invalid number of deadline entries" }
        require(coresCol.size >= size) { "Invalid number of core entries" }
    }

    public companion object {
        /**
         * Construct a [SimTrace] with the specified fragments.
         */
        public fun ofFragments(fragments: List<SimTraceFragment>): SimTrace {
            val size = fragments.size
            val usageCol = DoubleArray(size)
            val timestampCol = LongArray(size)
            val deadlineCol = LongArray(size)
            val coresCol = IntArray(size)

            for (i in fragments.indices) {
                val fragment = fragments[i]
                usageCol[i] = fragment.usage
                timestampCol[i] = fragment.timestamp
                deadlineCol[i] = fragment.timestamp + fragment.duration
                coresCol[i] = fragment.cores
            }

            return SimTrace(usageCol, timestampCol, deadlineCol, coresCol, size)
        }

        /**
         * Construct a [SimTrace] with the specified fragments.
         */
        @JvmStatic
        public fun ofFragments(vararg fragments: SimTraceFragment): SimTrace {
            val size = fragments.size
            val usageCol = DoubleArray(size)
            val timestampCol = LongArray(size)
            val deadlineCol = LongArray(size)
            val coresCol = IntArray(size)

            for (i in fragments.indices) {
                val fragment = fragments[i]
                usageCol[i] = fragment.usage
                timestampCol[i] = fragment.timestamp
                deadlineCol[i] = fragment.timestamp + fragment.duration
                coresCol[i] = fragment.cores
            }

            return SimTrace(usageCol, timestampCol, deadlineCol, coresCol, size)
        }

        /**
         * Create a [SimTrace.Builder] instance.
         */
        @JvmStatic
        public fun builder(): Builder = Builder()
    }

    /**
     * Construct a new [FlowSource] for the specified [cpu].
     *
     * @param cpu The [ProcessingUnit] for which to create the source.
     * @param offset The time offset to use for the trace.
     * @param fillMode The [FillMode] for filling missing data.
     */
    public fun newSource(cpu: ProcessingUnit, offset: Long, fillMode: FillMode = FillMode.None): FlowSource {
        return CpuConsumer(cpu, offset, fillMode, usageCol, timestampCol, deadlineCol, coresCol, size)
    }

    /**
     * An enumeration describing the modes for filling missing data.
     */
    public enum class FillMode {
        /**
         * When a gap in the trace data occurs, the CPU usage will be set to zero.
         */
        None,

        /**
         * When a gap in the trace data occurs, the previous CPU usage will be used.
         */
        Previous
    }

    /**
     * A builder class for a [SimTrace].
     */
    public class Builder internal constructor() {
        /**
         * The columns of the trace.
         */
        private var usageCol: DoubleArray = DoubleArray(16)
        private var timestampCol: LongArray = LongArray(16)
        private var deadlineCol: LongArray = LongArray(16)
        private var coresCol: IntArray = IntArray(16)

        /**
         * The number of entries in the trace.
         */
        private var size = 0

        /**
         * Add the specified [SimTraceFragment] to the trace.
         */
        public fun add(fragment: SimTraceFragment) {
            add(fragment.timestamp, fragment.timestamp + fragment.duration, fragment.usage, fragment.cores)
        }

        /**
         * Add a fragment to the trace.
         *
         * @param timestamp Timestamp at which the fragment starts (in epoch millis).
         * @param deadline Timestamp at which the fragment ends (in epoch millis).
         * @param usage CPU usage of this fragment.
         * @param cores Number of cores used.
         */
        public fun add(timestamp: Long, deadline: Long, usage: Double, cores: Int) {
            val size = size

            if (size == usageCol.size) {
                grow()
            }

            timestampCol[size] = timestamp
            deadlineCol[size] = deadline
            usageCol[size] = usage
            coresCol[size] = cores

            this.size++
        }

        /**
         * Helper function to grow the capacity of the column arrays.
         */
        private fun grow() {
            val arraySize = usageCol.size
            val newSize = arraySize * 2

            usageCol = usageCol.copyOf(newSize)
            timestampCol = timestampCol.copyOf(newSize)
            deadlineCol = deadlineCol.copyOf(newSize)
            coresCol = coresCol.copyOf(newSize)
        }

        /**
         * Construct the immutable [SimTrace].
         */
        public fun build(): SimTrace {
            return SimTrace(usageCol, timestampCol, deadlineCol, coresCol, size)
        }
    }

    /**
     * A CPU consumer for the trace workload.
     */
    private class CpuConsumer(
        cpu: ProcessingUnit,
        private val offset: Long,
        private val fillMode: FillMode,
        private val usageCol: DoubleArray,
        private val timestampCol: LongArray,
        private val deadlineCol: LongArray,
        private val coresCol: IntArray,
        private val size: Int
    ) : FlowSource {
        private val id = cpu.id
        private val coreCount = cpu.node.coreCount

        /**
         * The index in the trace.
         */
        private var _idx = 0

        override fun onPull(conn: FlowConnection, now: Long, delta: Long): Long {
            val size = size
            val nowOffset = now - offset

            var idx = _idx
            val deadlines = deadlineCol
            var deadline = deadlines[idx]

            while (deadline <= nowOffset && ++idx < size) {
                deadline = deadlines[idx]
            }

            if (idx >= size) {
                conn.close()
                return Long.MAX_VALUE
            }

            _idx = idx
            val timestamp = timestampCol[idx]

            // There is a gap in the trace, since the next fragment starts in the future.
            if (timestamp > nowOffset) {
                when (fillMode) {
                    FillMode.None -> conn.push(0.0) // Reset rate to zero
                    FillMode.Previous -> {} // Keep previous rate
                }
                return timestamp - nowOffset
            }

            val cores = min(coreCount, coresCol[idx])
            val usage = usageCol[idx]

            conn.push(if (id < cores) usage / cores else 0.0)
            return deadline - nowOffset
        }
    }
}
