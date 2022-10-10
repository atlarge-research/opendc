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

package org.opendc.experiments.compute.telemetry.table

import java.time.Instant

/**
 * An interface that is used to read a row of a service trace entry.
 */
public interface ServiceTableReader {
    /**
     * The timestamp of the current entry of the reader.
     */
    public val timestamp: Instant

    /**
     * The number of hosts that are up at this instant.
     */
    public val hostsUp: Int

    /**
     * The number of hosts that are down at this instant.
     */
    public val hostsDown: Int

    /**
     * The number of servers that are registered with the compute service..
     */
    public val serversTotal: Int

    /**
     * The number of servers that are pending to be scheduled.
     */
    public val serversPending: Int

    /**
     * The number of servers that are currently active.
     */
    public val serversActive: Int

    /**
     * The scheduling attempts that were successful.
     */
    public val attemptsSuccess: Int

    /**
     * The scheduling attempts that were unsuccessful due to client error.
     */
    public val attemptsFailure: Int

    /**
     * The scheduling attempts that were unsuccessful due to scheduler error.
     */
    public val attemptsError: Int
}
