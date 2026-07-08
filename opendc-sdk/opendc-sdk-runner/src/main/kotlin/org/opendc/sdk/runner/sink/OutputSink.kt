/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.runner.sink

import org.opendc.compute.simulator.telemetry.ComputeMonitor
import org.opendc.compute.simulator.telemetry.OutputFiles

/**
 * A destination for the metrics produced by a simulation run.
 *
 * A sink is a factory: it opens a fresh [SinkSession] for every run so that concurrent runs never
 * share mutable state. The runner records every metric table into the union of all sessions'
 * [SinkSession.monitor]s, so sinks compose freely — parquet, in-memory capture and callbacks can
 * all observe the same run.
 */
public fun interface OutputSink {
    /** Opens a session bound to a single run described by [context]. */
    public fun open(context: RunContext): SinkSession
}

/**
 * The per-run state of an [OutputSink].
 *
 * @property monitor The [ComputeMonitor] fed the run's metric records.
 * @property tables The metric tables this session needs recorded; the runner records the union
 *   across all sessions.
 */
public interface SinkSession {
    public val monitor: ComputeMonitor
    public val tables: Set<OutputFiles>

    /** The result harvested after the run completes, or `null` if the sink produces none. */
    public fun result(): SinkResult?
}
