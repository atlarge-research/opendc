/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.compute.simulator.telemetry.table.costModel

import org.opendc.simulator.compute.costmodel.CostModel
import java.time.Duration
import java.time.Instant

/**
 * An aggregator for task metrics before they are reported.
 */
public class CostModelTableReaderImpl(
    private val costModel: CostModel,
    private val startTime: Duration = Duration.ofMillis(0),
) : CostModelTableReader {
    override fun copy(): CostModelTableReader {
        val newCostModelTable =
            CostModelTableReaderImpl(
                costModel,
            )
        newCostModelTable.setValues(this)

        return newCostModelTable
    }

    override fun setValues(table: CostModelTableReader) {
        _timestamp = table.timestamp
        _timestampAbsolute = table.timestampAbsolute
        _energyCost = table.energyCost
    }

    public override val costModelInfo: CostModelInfo =
        CostModelInfo(
            costModel.energyCost
        )

    private var _timestamp = Instant.MIN
    override val timestamp: Instant
        get() = _timestamp

    private var _timestampAbsolute = Instant.MIN
    override val timestampAbsolute: Instant
        get() = _timestampAbsolute


    override val energyCost: Double
        get() = _energyCost - previousEnergyCost
    private var _energyCost = 0.0
    private var previousEnergyCost = 0.0

    /**
     * Record the next cycle.
     */
    override fun record(now: Instant) {
        _timestamp = now
        _timestampAbsolute = now + startTime

        costModel.updateCounters()
        _energyCost = costModel.energyCost
    }

    /**
     * Finish the aggregation for this cycle.
     */
    override fun reset() {
        previousEnergyCost = _energyCost
        _energyCost = 0.0

    }
}
