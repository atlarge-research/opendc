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
 * An interface that is used to read a row of a server trace entry.
 */
public interface ServerTableReader {
    /**
     * The timestamp of the current entry of the reader.
     */
    public val timestamp: Instant

    /**
     * The [ServerInfo] of the server to which the row belongs to.
     */
    public val server: ServerInfo

    /**
     * The [HostInfo] of the host on which the server is hosted or `null` if it has no host.
     */
    public val host: HostInfo?

    /**
     * The uptime of the host since last time in ms.
     */
    public val uptime: Long

    /**
     * The downtime of the host since last time in ms.
     */
    public val downtime: Long

    /**
     * The [Instant] at which the server was enqueued for the scheduler.
     */
    public val provisionTime: Instant?

    /**
     * The [Instant] at which the server booted.
     */
    public val bootTime: Instant?

    /**
     * The capacity of the CPUs of the servers (in MHz).
     */
    public val cpuLimit: Double

    /**
     * The duration (in seconds) that a CPU was active in the server.
     */
    public val cpuActiveTime: Long

    /**
     * The duration (in seconds) that a CPU was idle in the server.
     */
    public val cpuIdleTime: Long

    /**
     * The duration (in seconds) that a vCPU wanted to run, but no capacity was available.
     */
    public val cpuStealTime: Long

    /**
     * The duration (in seconds) of CPU time that was lost due to interference.
     */
    public val cpuLostTime: Long
}
