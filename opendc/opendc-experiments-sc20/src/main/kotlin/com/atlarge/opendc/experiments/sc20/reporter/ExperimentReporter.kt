/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.experiments.sc20.reporter

import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.virt.driver.VirtDriver
import java.io.Closeable

/**
 * A reporter used by experiments to report metrics.
 */
interface ExperimentReporter : Closeable {
    /**
     * This method is invoked when the state of a VM changes.
     */
    suspend fun reportVmStateChange(server: Server) {}

    /**
     * This method is invoked when the state of a host changes.
     */
    suspend fun reportHostStateChange(
        driver: VirtDriver,
        server: Server,
        submittedVms: Long,
        queuedVms: Long,
        runningVms: Long,
        finishedVms: Long
    ) {}

    /**
     * This method is invoked for a host for each slice that is finishes.
     */
    suspend fun reportHostSlice(
        time: Long,
        requestedBurst: Long,
        grantedBurst: Long,
        overcommissionedBurst: Long,
        interferedBurst: Long,
        cpuUsage: Double,
        cpuDemand: Double,
        numberOfDeployedImages: Int,
        hostServer: Server,
        submittedVms: Long,
        queuedVms: Long,
        runningVms: Long,
        finishedVms: Long,
        duration: Long = 5 * 60 * 1000L
    ) {}
}
