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

package org.opendc.faas.service

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.opendc.faas.service.deployer.FunctionInstance
import org.opendc.faas.service.telemetry.FunctionStats
import java.util.UUID

/**
 * An [FunctionObject] represents the service's view of a serverless function.
 */
public class FunctionObject(
    public val uid: UUID,
    name: String,
    allocatedMemory: Long,
    labels: Map<String, String>,
    meta: Map<String, Any>,
) : AutoCloseable {
    /**
     * Metrics tracked per function.
     */
    private var localInvocations = 0L
    private var localTimelyInvocations = 0L
    private var localDelayedInvocations = 0L
    private var localFailedInvocations = 0L
    private var localActiveInstances = 0
    private var localIdleInstances = 0
    private val localWaitTime =
        DescriptiveStatistics()
            .apply { windowSize = 100 }
    private val localActiveTime =
        DescriptiveStatistics()
            .apply { windowSize = 100 }

    /**
     * The instances associated with this function.
     */
    public val instances: MutableList<FunctionInstance> = mutableListOf()

    public var name: String = name
        private set

    public var memorySize: Long = allocatedMemory
        private set

    public val labels: MutableMap<String, String> = labels.toMutableMap()

    public val meta: MutableMap<String, Any> = meta.toMutableMap()

    /**
     * Report a scheduled invocation.
     */
    internal fun reportSubmission() {
        localInvocations++
    }

    /**
     * Report the deployment of an invocation.
     */
    internal fun reportDeployment(isDelayed: Boolean) {
        if (isDelayed) {
            localDelayedInvocations++
            localIdleInstances++
        } else {
            localTimelyInvocations++
        }
    }

    /**
     * Report the start of a function invocation.
     */
    internal fun reportStart(
        start: Long,
        submitTime: Long,
    ) {
        val wait = start - submitTime
        localWaitTime.addValue(wait.toDouble())

        localIdleInstances--
        localActiveInstances++
    }

    /**
     * Report the failure of a function invocation.
     */
    internal fun reportFailure() {
        localFailedInvocations++
    }

    /**
     * Report the end of a function invocation.
     */
    internal fun reportEnd(duration: Long) {
        localActiveTime.addValue(duration.toDouble())
        localIdleInstances++
        localActiveInstances--
    }

    /**
     * Collect the statistics of this function.
     */
    internal fun getStats(): FunctionStats {
        return FunctionStats(
            localInvocations,
            localTimelyInvocations,
            localDelayedInvocations,
            localFailedInvocations,
            localActiveInstances,
            localIdleInstances,
            localWaitTime.copy(),
            localActiveTime.copy(),
        )
    }

    override fun close() {
        val copy = instances.toList() // Make copy to prevent concurrent modification
        copy.forEach(FunctionInstance::close)
        instances.clear()
    }

    override fun equals(other: Any?): Boolean = other is FunctionObject && uid == other.uid

    override fun hashCode(): Int = uid.hashCode()
}
