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

package org.opendc.compute.simulator.telemetry.table

import org.opendc.trace.util.parquet.exporter.Exportable
import java.time.Instant

/**
 * An interface that is used to read a row of a host trace entry.
 */
public interface PowerSourceTableReader : Exportable {
    public fun copy(): PowerSourceTableReader

    public fun setValues(table: PowerSourceTableReader)

    public fun record(now: Instant)

    public fun reset()

    /**
     * The timestamp of the current entry of the reader relative to the start of the workload.
     */
    public val timestamp: Instant

    /**
     * The timestamp of the current entry of the reader.
     */
    public val timestampAbsolute: Instant

    /**
     * The number of connected hosts
     */
    public val hostsConnected: Int

    /**
     * The current power draw of the host in W.
     */
    public val powerDraw: Double

    /**
     * The total energy consumption of the host since last sample in J.
     */
    public val energyUsage: Double

    /**
     * The current carbon intensity of the host in gCO2 / kW.
     */
    public val carbonIntensity: Double

    /**
     * The current carbon emission since the last deadline in g.
     */
    public val carbonEmission: Double
}
