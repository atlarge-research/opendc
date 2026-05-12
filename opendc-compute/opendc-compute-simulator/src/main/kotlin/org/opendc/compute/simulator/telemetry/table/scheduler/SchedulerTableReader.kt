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

package org.opendc.compute.simulator.telemetry.table.scheduler

import org.opendc.trace.util.parquet.exporter.Exportable
import java.time.Instant

/**
 * An interface for reading a row of a portfolio scheduler decision trace entry.
 * Each row represents one policy's evaluation for a single scheduling decision.
 */
public interface SchedulerTableReader : Exportable {
    public fun copy(): SchedulerTableReader

    /**
     * The timestamp of the scheduling decision.
     */
    public val timestamp: Instant

    /**
     * The timestamp including the start time offset.
     */
    public val timestampAbsolute: Instant

    /**
     * A unique identifier for this scheduling decision.
     */
    public val decisionId: Long

    /**
     * The ID of the task being scheduled.
     */
    public val taskId: Int

    /**
     * The index of this policy in the portfolio.
     */
    public val policyIndex: Int

    /**
     * The name of the host that this policy would place the task on, or empty if no host found.
     */
    public val candidateHostName: String

    /**
     * The utility function score for this policy's proposed placement.
     * [Double.MAX_VALUE] if the policy could not find a host.
     */
    public val score: Double

    /**
     * Whether this policy was selected as the winner for this decision.
     */
    public val selected: Boolean

    /**
     * The overall utility function value at the moment of the decision
     * (the winning score).
     */
    public val winningScore: Double

    /**
     * The Disaster Recovery Risk score for this policy's proposed placement.
     */
    public val drrScore: Double

    /**
     * The Operational Risk score for this policy's proposed placement.
     */
    public val orScore: Double
}
