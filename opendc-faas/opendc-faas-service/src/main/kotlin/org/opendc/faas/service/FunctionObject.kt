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
import io.opentelemetry.api.metrics.BoundLongCounter
import io.opentelemetry.api.metrics.BoundLongHistogram
import io.opentelemetry.api.metrics.BoundLongUpDownCounter
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import org.opendc.faas.service.deployer.FunctionInstance
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
    public val attributes: Attributes = Attributes.builder()
        .put(ResourceAttributes.FAAS_ID, uid.toString())
        .put(ResourceAttributes.FAAS_NAME, name)
        .put(ResourceAttributes.FAAS_MAX_MEMORY, allocatedMemory)
        .put(AttributeKey.stringArrayKey("faas.labels"), labels.map { (k, v) -> "$k:$v" })
        .build()

    /**
     * The total amount of function invocations received by the function.
     */
    public val invocations: BoundLongCounter = meter.counterBuilder("function.invocations.total")
        .setDescription("Number of function invocations")
        .setUnit("1")
        .build()
        .bind(attributes)

    /**
     * The amount of function invocations that could be handled directly.
     */
    public val timelyInvocations: BoundLongCounter = meter.counterBuilder("function.invocations.warm")
        .setDescription("Number of function invocations handled directly")
        .setUnit("1")
        .build()
        .bind(attributes)

    /**
     * The amount of function invocations that were delayed due to function deployment.
     */
    public val delayedInvocations: BoundLongCounter = meter.counterBuilder("function.invocations.cold")
        .setDescription("Number of function invocations that are delayed")
        .setUnit("1")
        .build()
        .bind(attributes)

    /**
     * The amount of function invocations that failed.
     */
    public val failedInvocations: BoundLongCounter = meter.counterBuilder("function.invocations.failed")
        .setDescription("Number of function invocations that failed")
        .setUnit("1")
        .build()
        .bind(attributes)

    /**
     * The amount of instances for this function.
     */
    public val activeInstances: BoundLongUpDownCounter = meter.upDownCounterBuilder("function.instances.active")
        .setDescription("Number of active function instances")
        .setUnit("1")
        .build()
        .bind(attributes)

    /**
     * The amount of idle instances for this function.
     */
    public val idleInstances: BoundLongUpDownCounter = meter.upDownCounterBuilder("function.instances.idle")
        .setDescription("Number of idle function instances")
        .setUnit("1")
        .build()
        .bind(attributes)

    /**
     * The time that the function waited.
     */
    public val waitTime: BoundLongHistogram = meter.histogramBuilder("function.time.wait")
        .ofLongs()
        .setDescription("Time the function has to wait before being started")
        .setUnit("ms")
        .build()
        .bind(attributes)

    /**
     * The time that the function was running.
     */
    public val activeTime: BoundLongHistogram = meter.histogramBuilder("function.time.active")
        .ofLongs()
        .setDescription("Time the function was running")
        .setUnit("ms")
        .build()
        .bind(attributes)

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

    override fun close() {
        instances.forEach(FunctionInstance::close)
        instances.clear()
    }

    override fun equals(other: Any?): Boolean = other is FunctionObject && uid == other.uid

    override fun hashCode(): Int = uid.hashCode()
}
