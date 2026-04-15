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

package org.opendc.compute.simulator.scheduler.portfolio

/**
 * A record of a single policy's evaluation within a portfolio scheduling decision.
 *
 * @param decisionId A unique identifier for the scheduling decision.
 * @param timestampMs The simulation time in milliseconds when the decision was made.
 * @param taskId The ID of the task being scheduled.
 * @param policyIndex The index of this policy in the portfolio.
 * @param candidateHostName The host this policy proposed, or empty if none.
 * @param score The utility function score for this placement.
 * @param selected Whether this policy was chosen.
 * @param winningScore The best (lowest) score among all policies for this decision.
 */
public data class SchedulingDecisionRecord(
    val decisionId: Long,
    val timestampMs: Long,
    val taskId: Int,
    val policyIndex: Int,
    val candidateHostName: String,
    val score: Double,
    val selected: Boolean,
    val winningScore: Double,
    val drrScore: Double,
    val orScore: Double,
)
