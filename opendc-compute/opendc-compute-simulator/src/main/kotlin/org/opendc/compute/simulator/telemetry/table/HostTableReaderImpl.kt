package org.opendc.compute.simulator.telemetry.table

import org.opendc.compute.carbon.CarbonTrace
import org.opendc.compute.simulator.host.SimHost
import java.time.Duration
import java.time.Instant

/**
 * An aggregator for host metrics before they are reported.
 */
public class HostTableReaderImpl(
    host: SimHost,
    private val startTime: Duration = Duration.ofMillis(0),
    private val carbonTrace: CarbonTrace = CarbonTrace(null),
) : HostTableReader {
    override fun copy(): HostTableReader {
        val newHostTable =
            HostTableReaderImpl(_host)
        newHostTable.setValues(this)

        return newHostTable
    }

    override fun setValues(table: HostTableReader) {
        _timestamp = table.timestamp
        _timestampAbsolute = table.timestampAbsolute

        _guestsTerminated = table.guestsTerminated
        _guestsRunning = table.guestsRunning
        _guestsError = table.guestsError
        _guestsInvalid = table.guestsInvalid
        _cpuLimit = table.cpuLimit
        _cpuDemand = table.cpuDemand
        _cpuUsage = table.cpuUsage
        _cpuUtilization = table.cpuUtilization
        _cpuActiveTime = table.cpuActiveTime
        _cpuIdleTime = table.cpuIdleTime
        _cpuStealTime = table.cpuStealTime
        _cpuLostTime = table.cpuLostTime
        _powerDraw = table.powerDraw
        _energyUsage = table.energyUsage
        _carbonIntensity = table.carbonIntensity
        _carbonEmission = table.carbonEmission
        _uptime = table.uptime
        _downtime = table.downtime
        _bootTime = table.bootTime
        _bootTimeAbsolute = table.bootTimeAbsolute
    }

    private val _host = host

    override val host: HostInfo =
        HostInfo(
            host.getUid().toString(),
            host.getName(),
            "x86",
            host.getModel().coreCount,
            host.getModel().cpuCapacity,
            host.getModel().memoryCapacity,
        )

    override val timestamp: Instant
        get() = _timestamp
    private var _timestamp = Instant.MIN

    override val timestampAbsolute: Instant
        get() = _timestampAbsolute
    private var _timestampAbsolute = Instant.MIN

    override val guestsTerminated: Int
        get() = _guestsTerminated
    private var _guestsTerminated = 0

    override val guestsRunning: Int
        get() = _guestsRunning
    private var _guestsRunning = 0

    override val guestsError: Int
        get() = _guestsError
    private var _guestsError = 0

    override val guestsInvalid: Int
        get() = _guestsInvalid
    private var _guestsInvalid = 0

    override val cpuLimit: Double
        get() = _cpuLimit
    private var _cpuLimit = 0.0

    override val cpuUsage: Double
        get() = _cpuUsage
    private var _cpuUsage = 0.0

    override val cpuDemand: Double
        get() = _cpuDemand
    private var _cpuDemand = 0.0

    override val cpuUtilization: Double
        get() = _cpuUtilization
    private var _cpuUtilization = 0.0

    override val cpuActiveTime: Long
        get() = _cpuActiveTime - previousCpuActiveTime
    private var _cpuActiveTime = 0L
    private var previousCpuActiveTime = 0L

    override val cpuIdleTime: Long
        get() = _cpuIdleTime - previousCpuIdleTime
    private var _cpuIdleTime = 0L
    private var previousCpuIdleTime = 0L

    override val cpuStealTime: Long
        get() = _cpuStealTime - previousCpuStealTime
    private var _cpuStealTime = 0L
    private var previousCpuStealTime = 0L

    override val cpuLostTime: Long
        get() = _cpuLostTime - previousCpuLostTime
    private var _cpuLostTime = 0L
    private var previousCpuLostTime = 0L

    override val powerDraw: Double
        get() = _powerDraw
    private var _powerDraw = 0.0

    override val energyUsage: Double
        get() = _energyUsage - previousEnergyUsage
    private var _energyUsage = 0.0
    private var previousEnergyUsage = 0.0

    override val carbonIntensity: Double
        get() = _carbonIntensity
    private var _carbonIntensity = 0.0

    override val carbonEmission: Double
        get() = _carbonEmission
    private var _carbonEmission = 0.0

    override val uptime: Long
        get() = _uptime - previousUptime
    private var _uptime = 0L
    private var previousUptime = 0L

    override val downtime: Long
        get() = _downtime - previousDowntime
    private var _downtime = 0L
    private var previousDowntime = 0L

    override val bootTime: Instant?
        get() = _bootTime
    private var _bootTime: Instant? = null

    override val bootTimeAbsolute: Instant?
        get() = _bootTimeAbsolute
    private var _bootTimeAbsolute: Instant? = null

    /**
     * Record the next cycle.
     */
    override fun record(now: Instant) {
        val hostCpuStats = _host.getCpuStats()
        val hostSysStats = _host.getSystemStats()

        _timestamp = now
        _timestampAbsolute = now + startTime

        _guestsTerminated = hostSysStats.guestsTerminated
        _guestsRunning = hostSysStats.guestsRunning
        _guestsError = hostSysStats.guestsError
        _guestsInvalid = hostSysStats.guestsInvalid
        _cpuLimit = hostCpuStats.capacity
        _cpuDemand = hostCpuStats.demand
        _cpuUsage = hostCpuStats.usage
        _cpuUtilization = hostCpuStats.utilization
        _cpuActiveTime = hostCpuStats.activeTime
        _cpuIdleTime = hostCpuStats.idleTime
        _cpuStealTime = hostCpuStats.stealTime
        _cpuLostTime = hostCpuStats.lostTime
        _powerDraw = hostSysStats.powerDraw
        _energyUsage = hostSysStats.energyUsage
        _carbonIntensity = carbonTrace.getCarbonIntensity(timestampAbsolute)

        _carbonEmission = carbonIntensity * (energyUsage / 3600000.0) // convert energy usage from J to kWh
        _uptime = hostSysStats.uptime.toMillis()
        _downtime = hostSysStats.downtime.toMillis()
        _bootTime = hostSysStats.bootTime
        _bootTime = hostSysStats.bootTime + startTime
    }

    /**
     * Finish the aggregation for this cycle.
     */
    override fun reset() {
        // Reset intermediate state for next aggregation
        previousCpuActiveTime = _cpuActiveTime
        previousCpuIdleTime = _cpuIdleTime
        previousCpuStealTime = _cpuStealTime
        previousCpuLostTime = _cpuLostTime
        previousEnergyUsage = _energyUsage
        previousUptime = _uptime
        previousDowntime = _downtime

        _guestsTerminated = 0
        _guestsRunning = 0
        _guestsError = 0
        _guestsInvalid = 0

        _cpuLimit = 0.0
        _cpuUsage = 0.0
        _cpuDemand = 0.0
        _cpuUtilization = 0.0

        _powerDraw = 0.0
        _energyUsage = 0.0
        _carbonIntensity = 0.0
        _carbonEmission = 0.0
    }
}
