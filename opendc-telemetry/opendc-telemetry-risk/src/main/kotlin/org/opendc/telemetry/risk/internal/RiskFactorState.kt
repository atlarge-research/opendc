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

package org.opendc.telemetry.risk.internal

import org.opendc.telemetry.risk.RiskFactor
import org.opendc.telemetry.risk.RiskMonitorListener
import java.time.Clock
import java.time.Instant

/**
 * The internal state associated with each risk factor registered per [RiskMonitorImpl].
 */
internal class RiskFactorState(@JvmField val factor: RiskFactor) {
    /**
     * The next target for the factor.
     */
    @JvmField var target: Instant = Instant.MIN

    /**
     * Determine whether the factor should be evaluated at [now].
     */
    fun shouldEvaluate(now: Instant): Boolean = now >= target

    /**
     * Evaluate the objective of the risk factor.
     */
    fun evaluate(now: Instant, listeners: List<RiskMonitorListener>) {
        val points = factor.indicator.collect(now.minusSeconds(1))
        for (point in points) {
            val isViolation = factor.objective.test(point)
            for (listener in listeners) {
                listener.onEvaluation(factor, point, isViolation)
            }
        }
    }

    /**
     * Determine the next [Instant] at which the factor should be re-evaluated.
     */
    fun updateTarget(clock: Clock, now: Instant) {
        target = factor.period.getNextPeriod(now.atZone(clock.zone)).toInstant()
        factor.indicator.reset(now)
    }
}
