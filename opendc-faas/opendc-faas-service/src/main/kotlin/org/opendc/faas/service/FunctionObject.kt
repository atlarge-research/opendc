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

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.LongHistogram
import io.opentelemetry.api.metrics.LongUpDownCounter
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.opendc.faas.service.deployer.FunctionInstance
import org.opendc.faas.service.telemetry.FunctionStats
import java.util.*

/**
 * An [FunctionObject] represents the service's view of a serverless function.
 */
public class FunctionObject(
    meter: Meter,
    public val uid: UUID,
    name: String,
    allocatedMemory: Long,
    labels: Map<String, String>,
    meta: Map<String, Any>
) : AutoCloseable {
    /**
     * The attributes of this function.
     */
    private val attributes: Attributes = Attributes.builder()
        .put(ResourceAttributes.FAAS_ID, uid.toString())
        .put(ResourceAttributes.FAAS_NAME, name)
        .put(ResourceAttributes.FAAS_MAX_MEMORY, allocatedMemory)
        .put(AttributeKey.stringArrayKey("faas.labels"), labels.map { (k, v) -> "$k:$v" })
        .build()

    /**
     * The total amount of function invocations received by the function.
     */
    private val invocations: LongCounter = meter.counterBuilder("function.invocations.total")
        .setDescription("Number of function invocations")
        .setUnit("1")
        .build()
    private var _invocations = 0L

    /**
     * The amount of function invocations that could be handled directly.
     */
    private val timelyInvocations: LongCounter = meter.counterBuilder("function.invocations.warm")
        .setDescription("Number of function invocations handled directly")
        .setUnit("1")
        .build()
    private var _timelyInvocations = 0L

    /**
     * The amount of function invocations that were delayed due to function deployment.
     */
    private val delayedInvocations: LongCounter = meter.counterBuilder("function.invocations.cold")
        .setDescription("Number of function invocations that are delayed")
        .setUnit("1")
        .build()
    private var _delayedInvocations = 0L

    /**
     * The amount of function invocations that failed.
     */
    private val failedInvocations: LongCounter = meter.counterBuilder("function.invocations.failed")
        .setDescription("Number of function invocations that failed")
        .setUnit("1")
        .build()
    private var _failedInvocations = 0L

    /**
     * The amount of instances for this function.
     */
    private val activeInstances: LongUpDownCounter = meter.upDownCounterBuilder("function.instances.active")
        .setDescription("Number of active function instances")
        .setUnit("1")
        .build()
    private var _activeInstances = 0

    /**
     * The amount of idle instances for this function.
     */
    private val idleInstances: LongUpDownCounter = meter.upDownCounterBuilder("function.instances.idle")
        .setDescription("Number of idle function instances")
        .setUnit("1")
        .build()
    private var _idleInstances = 0

    /**
     * The time that the function waited.
     */
    private val waitTime: LongHistogram = meter.histogramBuilder("function.time.wait")
        .ofLongs()
        .setDescription("Time the function has to wait before being started")
        .setUnit("ms")
        .build()
    private val _waitTime = DescriptiveStatistics()
        .apply { windowSize = 100 }

    /**
     * The time that the function was running.
     */
    private val activeTime: LongHistogram = meter.histogramBuilder("function.time.active")
        .ofLongs()
        .setDescription("Time the function was running")
        .setUnit("ms")
        .build()
    private val _activeTime = DescriptiveStatistics()
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
        invocations.add(1, attributes)
        _invocations++
    }

    /**
     * Report the deployment of an invocation.
     */
    internal fun reportDeployment(isDelayed: Boolean) {
        if (isDelayed) {
            delayedInvocations.add(1, attributes)
            _delayedInvocations++

            idleInstances.add(1, attributes)
            _idleInstances++
        } else {
            timelyInvocations.add(1, attributes)
            _timelyInvocations++
        }
    }

    /**
     * Report the start of a function invocation.
     */
    internal fun reportStart(start: Long, submitTime: Long) {
        val wait = start - submitTime
        waitTime.record(wait, attributes)
        _waitTime.addValue(wait.toDouble())

        idleInstances.add(-1, attributes)
        _idleInstances--
        activeInstances.add(1, attributes)
        _activeInstances++
    }

    /**
     * Report the failure of a function invocation.
     */
    internal fun reportFailure() {
        failedInvocations.add(1, attributes)
        _failedInvocations++
    }

    /**
     * Report the end of a function invocation.
     */
    internal fun reportEnd(duration: Long) {
        activeTime.record(duration, attributes)
        _activeTime.addValue(duration.toDouble())
        idleInstances.add(1, attributes)
        _idleInstances++
        activeInstances.add(-1, attributes)
        _activeInstances--
    }

    /**
     * Collect the statistics of this function.
     */
    internal fun getStats(): FunctionStats {
        return FunctionStats(
            _invocations,
            _timelyInvocations,
            _delayedInvocations,
            _failedInvocations,
            _activeInstances,
            _idleInstances,
            _waitTime.copy(),
            _activeTime.copy()
        )
    }

    override fun close() {
        instances.forEach(FunctionInstance::close)
        instances.clear()
    }

    override fun equals(other: Any?): Boolean = other is FunctionObject && uid == other.uid

    override fun hashCode(): Int = uid.hashCode()
}
