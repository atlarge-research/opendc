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

import org.opendc.compute.simulator.scheduler.portfolio.SchedulingDecisionRecord
import java.time.Duration
import java.time.Instant

/**
 * Implementation of [SchedulerTableReader] that wraps a [SchedulingDecisionRecord].
 */
public class SchedulerTableReaderImpl(
    private val startTime: Duration = Duration.ofMillis(0),
) : SchedulerTableReader {
    private var _timestamp: Instant = Instant.MIN
    private var _timestampAbsolute: Instant = Instant.MIN
    private var _decisionId: Long = 0
    private var _taskId: Int = 0
    private var _policyIndex: Int = 0
    private var _candidateHostName: String = ""
    private var _score: Double = 0.0
    private var _selected: Boolean = false
    private var _winningScore: Double = 0.0
    private var _drrScore: Double = 0.0
    private var _orScore: Double = 0.0

    override val timestamp: Instant get() = _timestamp
    override val timestampAbsolute: Instant get() = _timestampAbsolute
    override val decisionId: Long get() = _decisionId
    override val taskId: Int get() = _taskId
    override val policyIndex: Int get() = _policyIndex
    override val candidateHostName: String get() = _candidateHostName
    override val score: Double get() = _score
    override val selected: Boolean get() = _selected
    override val winningScore: Double get() = _winningScore
    override val drrScore: Double get() = _drrScore
    override val orScore: Double get() = _orScore

    public fun setFrom(record: SchedulingDecisionRecord) {
        _timestamp = Instant.ofEpochMilli(record.timestampMs)
        _timestampAbsolute = Instant.ofEpochMilli(record.timestampMs) + startTime
        _decisionId = record.decisionId
        _taskId = record.taskId
        _policyIndex = record.policyIndex
        _candidateHostName = record.candidateHostName
        _score = record.score
        _selected = record.selected
        _winningScore = record.winningScore
        _drrScore = record.drrScore
        _orScore = record.orScore
    }

    override fun copy(): SchedulerTableReader {
        val copy = SchedulerTableReaderImpl(startTime)
        copy._timestamp = _timestamp
        copy._timestampAbsolute = _timestampAbsolute
        copy._decisionId = _decisionId
        copy._taskId = _taskId
        copy._policyIndex = _policyIndex
        copy._candidateHostName = _candidateHostName
        copy._score = _score
        copy._selected = _selected
        copy._winningScore = _winningScore
        copy._drrScore = _drrScore
        copy._orScore = _orScore
        return copy
    }
}
