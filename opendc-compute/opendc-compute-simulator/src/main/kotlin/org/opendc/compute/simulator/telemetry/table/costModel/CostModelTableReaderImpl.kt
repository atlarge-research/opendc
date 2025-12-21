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
        _hostName = table.hostName
        _timestamp = table.timestamp
        _timestampAbsolute = table.timestampAbsolute
        _energyCost = table.energyCost
        _generalCost = table.generalCost
        _employeeCost = table.employeeCost
        _componentDegradationCost = table.componentDegradationCost
    }

    public override val costModelInfo: CostModelInfo =
        CostModelInfo(
            costModel.hostName,
            costModel.energyCost,
            costModel.employeeCost,
            costModel.generalCost,
            costModel.hardwareDegradationCost,
        )

    override val hostName: String
        get () = _hostName
    private var _hostName : String = ""

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

    override val employeeCost: Double
        get() = _employeeCost - previousEmployeeCost
    private var _employeeCost = 0.0
    private var previousEmployeeCost = 0.0

    override val generalCost: Double
        get() = _generalCost - previousGeneralCost
    private var _generalCost = 0.0
    private var previousGeneralCost = 0.0

    override val componentDegradationCost: Double
        get() = _componentDegradationCost - previousValueDegradationCost
    private var _componentDegradationCost = 0.0
    private var previousValueDegradationCost = 0.0


    /**
     * Record the next cycle.
     */
    override fun record(now: Instant) {
        _timestamp = now
        _timestampAbsolute = now + startTime
        _hostName = costModel.hostName

        costModel.updateCounters()
        _energyCost = costModel.energyCost
        _employeeCost = costModel.employeeCost
        _generalCost = costModel.generalCost
        _componentDegradationCost = costModel.hardwareDegradationCost
    }

    /**
     * Finish the aggregation for this cycle.
     */
    override fun reset() {
        previousEnergyCost = _energyCost
        _energyCost = 0.0
        previousEmployeeCost = _employeeCost
        _employeeCost = 0.0
        previousGeneralCost = _generalCost
        _generalCost = 0.0
        previousValueDegradationCost = _componentDegradationCost
        _componentDegradationCost = 0.0
    }
}
