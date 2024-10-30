package org.opendc.compute.simulator.telemetry.table

import org.opendc.compute.carbon.CarbonTrace
import org.opendc.simulator.compute.power.SimPowerSource
import java.time.Duration
import java.time.Instant

/**
 * An aggregator for task metrics before they are reported.
 */
public class PowerSourceTableReaderImpl(
    powerSource: SimPowerSource,
    private val startTime: Duration = Duration.ofMillis(0),
    private val carbonTrace: CarbonTrace = CarbonTrace(null),
) : PowerSourceTableReader {
    override fun copy(): PowerSourceTableReader {
        val newPowerSourceTable =
            PowerSourceTableReaderImpl(
                powerSource
            )
        newPowerSourceTable.setValues(this)

        return newPowerSourceTable
    }

    override fun setValues(table: PowerSourceTableReader) {
        _timestamp = table.timestamp
        _timestampAbsolute = table.timestampAbsolute

        _hostsConnected = table.hostsConnected
        _powerDraw = table.powerDraw
        _energyUsage = table.energyUsage
        _carbonIntensity = table.carbonIntensity
        _carbonEmission = table.carbonEmission
    }

    private val powerSource = powerSource

    private var _timestamp = Instant.MIN
    override val timestamp: Instant
        get() = _timestamp

    private var _timestampAbsolute = Instant.MIN
    override val timestampAbsolute: Instant
        get() = _timestampAbsolute

    override val hostsConnected: Int
        get() = _hostsConnected
    private var _hostsConnected: Int = 0

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

    /**
     * Record the next cycle.
     */
    override fun record(now: Instant) {

        _timestamp = now
        _timestampAbsolute = now + startTime

        _hostsConnected = 0
        _powerDraw = powerSource.powerDraw
        _energyUsage = powerSource.energyUsage
        _carbonIntensity = carbonTrace.getCarbonIntensity(timestampAbsolute)
        _carbonEmission = carbonIntensity * (energyUsage / 3600000.0)
    }

    /**
     * Finish the aggregation for this cycle.
     */
    override fun reset() {

        previousEnergyUsage = _energyUsage

        _hostsConnected = 0
        _powerDraw = 0.0
        _energyUsage = 0.0
        _carbonIntensity = 0.0
        _carbonEmission = 0.0
    }
}
