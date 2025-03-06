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

package org.opendc.compute.simulator.telemetry.table.battery

import org.opendc.simulator.compute.power.batteries.BatteryState
import org.opendc.trace.util.parquet.exporter.Exportable
import java.time.Instant

/**
 * An interface that is used to read a row of a host trace entry.
 */
public interface BatteryTableReader : Exportable {
    public fun copy(): BatteryTableReader

    public fun setValues(table: BatteryTableReader)

    public fun record(now: Instant)

    public fun reset()

    public val batteryInfo: BatteryInfo

    /**
     * The timestamp of the current entry of the reader relative to the start of the workload.
     */
    public val timestamp: Instant

    /**
     * The timestamp of the current entry of the reader.
     */
    public val timestampAbsolute: Instant

    /**
     * The current power draw of the host in W.
     */
    public val powerDraw: Double

    /**
     * The current power draw of the host in W.
     */
    public val energyUsage: Double

    /**
     * The embodied carbon cost of the Battery in kg
     */
    public val embodiedCarbonEmission: Double

    /**
     * The current state of the battery
     */
    public val batteryState: BatteryState

    /**
     * The current charge of the battery in J
     */
    public val charge: Double

    /**
     * The capacity of the battery in J
     */
    public val capacity: Double
}
