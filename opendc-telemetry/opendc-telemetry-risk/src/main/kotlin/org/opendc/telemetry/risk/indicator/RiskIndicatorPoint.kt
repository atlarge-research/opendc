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

package org.opendc.telemetry.risk.indicator

import java.time.Instant

/**
 * An instantiation of a [RiskIndicator] at a moment in time.
 */
public sealed class RiskIndicatorPoint {
    /**
     * A key describing the group to which the indicator belongs.
     */
    public abstract val key: Any?

    /**
     * The timestamp at which the point was recorded.
     */
    public abstract val timestamp: Instant

    /**
     * The value of the risk indicator that is compared to the objective.
     */
    public abstract val value: Double

    /**
     * A ratio between two metrics.
     */
    public data class Ratio(
        override val key: Any?,
        override val timestamp: Instant,
        public val partial: Double,
        public val total: Double,
        override val value: Double = partial / total
    ) : RiskIndicatorPoint()

    /**
     * A single [value].
     */
    public data class Single(
        override val key: Any?,
        override val timestamp: Instant,
        override val value: Double
    ) : RiskIndicatorPoint()
}
