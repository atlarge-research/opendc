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

package org.opendc.compute.simulator.telemetry.table.service

import org.opendc.trace.util.parquet.exporter.Exportable
import java.time.Instant

/**
 * An interface that is used to read a row of a service trace entry.
 */
public interface ServiceTableReader : Exportable {
    public fun copy(): ServiceTableReader

    public fun setValues(table: ServiceTableReader)

    public fun record(now: Instant)

    /**
     * The timestamp of the current entry of the reader.
     */
    public val timestamp: Instant

    /**
     * The timestamp of the current entry of the reader.
     */
    public val timestampAbsolute: Instant

    /**
     * The number of hosts that are up at this instant.
     */
    public val hostsUp: Int

    /**
     * The number of hosts that are down at this instant.
     */
    public val hostsDown: Int

    /**
     * The number of tasks that are registered with the compute service.
     */
    public val tasksTotal: Int

    /**
     * The number of tasks that are pending to be scheduled.
     */
    public val tasksPending: Int

    /**
     * The number of tasks that are currently active.
     */
    public val tasksActive: Int

    /**
     * The number of tasks that completed the tasks successfully
     */
    public val tasksCompleted: Int

    /**
     * The number of tasks that failed more times than allowed and are thus terminated
     */
    public val tasksTerminated: Int

    /**
     * The scheduling attempts that were successful.
     */
    public val attemptsSuccess: Int

    /**
     * The scheduling attempts that were unsuccessful due to client error.
     */
    public val attemptsFailure: Int
}
