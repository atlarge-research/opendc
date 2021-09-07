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

package org.opendc.telemetry.risk

import org.opendc.telemetry.risk.cost.RiskCostModel
import org.opendc.telemetry.risk.indicator.RiskIndicator

/**
 * A risk factor models the cost of risk in a datacenter, by comparing aggregated metrics against an objective.
 *
 * @param id A unique identifier for the risk factor.
 * @param indicator The risk indicator to quantify the risk.
 * @param objective The objective to evaluate for this factor.
 * @param cost A cost model for the violation of this risk factor.
 * @param period The compliance period over which this factor is evaluated.
 */
public data class RiskFactor(
    public val id: String,
    public val indicator: RiskIndicator,
    public val objective: RiskObjective,
    public val cost: RiskCostModel,
    public val period: RiskEvaluationPeriod
) {
    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean = other is RiskFactor && id == other.id
}
