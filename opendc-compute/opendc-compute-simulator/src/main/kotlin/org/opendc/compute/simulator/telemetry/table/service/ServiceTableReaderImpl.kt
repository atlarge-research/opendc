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

package org.opendc.compute.simulator.telemetry.table.service

import org.opendc.compute.simulator.service.ComputeService
import java.time.Duration
import java.time.Instant

/**
 * An aggregator for service metrics before they are reported.
 */
public class ServiceTableReaderImpl(
    private val service: ComputeService,
    private val startTime: Duration = Duration.ofMillis(0),
) : ServiceTableReader {
    override fun copy(): ServiceTableReader {
        val newServiceTable =
            ServiceTableReaderImpl(
                service,
            )
        newServiceTable.setValues(this)

        return newServiceTable
    }

    override fun setValues(table: ServiceTableReader) {
        _timestamp = table.timestamp
        _timestampAbsolute = table.timestampAbsolute

        _hostsUp = table.hostsUp
        _hostsDown = table.hostsDown
        _tasksTotal = table.tasksTotal
        _tasksPending = table.tasksPending
        _tasksActive = table.tasksActive
        _tasksCompleted = table.tasksCompleted
        _tasksTerminated = table.tasksTerminated
        _attemptsSuccess = table.attemptsSuccess
        _attemptsFailure = table.attemptsFailure
    }

    private var _timestamp: Instant = Instant.MIN
    override val timestamp: Instant
        get() = _timestamp

    private var _timestampAbsolute: Instant = Instant.MIN
    override val timestampAbsolute: Instant
        get() = _timestampAbsolute

    override val hostsUp: Int
        get() = _hostsUp
    private var _hostsUp = 0

    override val hostsDown: Int
        get() = _hostsDown
    private var _hostsDown = 0

    override val tasksTotal: Int
        get() = _tasksTotal
    private var _tasksTotal = 0

    override val tasksPending: Int
        get() = _tasksPending
    private var _tasksPending = 0

    override val tasksCompleted: Int
        get() = _tasksCompleted
    private var _tasksCompleted = 0

    override val tasksActive: Int
        get() = _tasksActive
    private var _tasksActive = 0

    override val tasksTerminated: Int
        get() = _tasksTerminated
    private var _tasksTerminated = 0

    override val attemptsSuccess: Int
        get() = _attemptsSuccess
    private var _attemptsSuccess = 0

    override val attemptsFailure: Int
        get() = _attemptsFailure
    private var _attemptsFailure = 0

    /**
     * Record the next cycle.
     */
    override fun record(now: Instant) {
        _timestamp = now
        _timestampAbsolute = now + startTime

        val stats = service.getSchedulerStats()
        _hostsUp = stats.hostsAvailable
        _hostsDown = stats.hostsUnavailable
        _tasksTotal = stats.tasksTotal
        _tasksPending = stats.tasksPending
        _tasksCompleted = stats.tasksCompleted
        _tasksActive = stats.tasksActive
        _tasksTerminated = stats.tasksTerminated
        _attemptsSuccess = stats.attemptsSuccess.toInt()
        _attemptsFailure = stats.attemptsFailure.toInt()
    }
}
