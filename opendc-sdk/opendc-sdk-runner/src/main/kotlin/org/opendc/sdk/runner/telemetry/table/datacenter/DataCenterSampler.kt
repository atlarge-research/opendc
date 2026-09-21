/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.sdk.runner.telemetry.table.datacenter

import org.opendc.compute.simulator.datacenter.SimDataCenter
import java.time.Duration
import java.time.Instant

public class DataCenterSampler(
    private val startTime: Duration = Duration.ofMillis(0),
) {
    public fun sample(
        now: Instant,
        dataCenter: SimDataCenter,
    ): DataCenterSample {
        val dataCenterSystemStats = dataCenter.getSystemStats()

        val timestampAbsolute = now + startTime

        // energy & carbon stats
        val powerDraw = dataCenterSystemStats.powerDraw
        val energyUsage = dataCenterSystemStats.energyUsage
        val carbonIntensity = dataCenterSystemStats.carbonIntensity
        val carbonEmission = dataCenterSystemStats.carbonEmission

        // TODO: Determine if this metric still makes sense in this form
        val embodiedCarbon = dataCenterSystemStats.embodiedCarbon

        return DataCenterSample(
            dataCenterName = dataCenter.getName(),
            timestamp = now,
            timestampAbsolute = timestampAbsolute,
            powerDraw = powerDraw,
            energyUsage = energyUsage,
            carbonIntensity = carbonIntensity,
            carbonEmission = carbonEmission,
        )
    }
}
