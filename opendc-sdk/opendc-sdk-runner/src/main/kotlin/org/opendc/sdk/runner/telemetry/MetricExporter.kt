/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.sdk.runner.telemetry

import org.opendc.sdk.runner.telemetry.table.battery.BatterySample
import org.opendc.sdk.runner.telemetry.table.cluster.ClusterSample
import org.opendc.sdk.runner.telemetry.table.datacenter.DataCenterSample
import org.opendc.sdk.runner.telemetry.table.host.HostSample
import org.opendc.sdk.runner.telemetry.table.powerSource.PowerSourceSample
import org.opendc.sdk.runner.telemetry.table.service.ServiceSample
import org.opendc.sdk.runner.telemetry.table.simulation.SimulationMeta
import org.opendc.sdk.runner.telemetry.table.task.TaskMeta
import org.opendc.sdk.runner.telemetry.table.task.TaskSample
import org.opendc.sdk.runner.telemetry.table.topology.TopologyMeta

/**
 * A monitor that exports Samples.
 */
public interface MetricExporter {
    /**
     * Record an entry with the specified [reader].
     */
    public fun export(reader: BatterySample) {}

    /**
     * Record an entry with the specified [reader].
     */
    public fun export(reader: ClusterSample) {}

    /**
     * Record an entry with the specified [reader].
     */
    public fun export(reader: DataCenterSample) {}

    /**
     * Record an entry with the specified [reader].
     */
    public fun export(reader: HostSample) {}

    /**
     * Record an entry with the specified [reader].
     */
    public fun export(reader: PowerSourceSample) {}

    /**
     * Record an entry with the specified [reader].
     */
    public fun export(reader: ServiceSample) {}

    /**
     * Record an entry with the specified [reader].
     */
    public fun export(reader: TaskSample) {}

    /**
     * Export the run-level attributes of the simulation, such as the absolute time it starts at. Every exported time is
     * relative to that start. It is exported once, before any sample.
     */
    public fun export(meta: SimulationMeta) {}

    /**
     * Export the topology of the run, which maps the ids in the samples to names, parents and static attributes. It is
     * exported once, before any sample.
     */
    public fun export(meta: TopologyMeta) {}

    /**
     * Export the static attributes of a task. They are exported once per task, when the task is deleted or, for tasks
     * still in the service, when the run ends.
     */
    public fun export(meta: TaskMeta) {}
}
