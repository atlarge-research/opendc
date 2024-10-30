package org.opendc.compute.simulator.telemetry.table

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
