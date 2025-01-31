package org.opendc.simulator.compute.power.batteries.policy;

import org.opendc.simulator.compute.power.batteries.BatteryAggregator;
import org.opendc.simulator.compute.power.batteries.BatteryState;
import org.opendc.simulator.compute.power.batteries.SimBattery;
import org.opendc.simulator.engine.graph.FlowGraph;

public class DoubleThresholdBatteryPolicy extends BatteryPolicy {

    private final double carbonThreshold;

    /**
     *
     * @param parentGraph     The {@link FlowGraph} this stage belongs to.
     * @param battery
     * @param aggregator
     * @param carbonThreshold
     */
    public DoubleThresholdBatteryPolicy(
        FlowGraph parentGraph, SimBattery battery, BatteryAggregator aggregator, double carbonThreshold) {
        super(parentGraph, battery, aggregator);

        this.carbonThreshold = carbonThreshold;
    }

    @Override
    public long onUpdate(long now) {

        if (this.carbonIntensity >= this.carbonThreshold & !this.battery.isEmpty()) {
            this.setBatteryState(BatteryState.Discharging);
            return Long.MAX_VALUE;
        }

        if (this.carbonIntensity < this.carbonThreshold & !this.battery.isFull()) {
            this.setBatteryState(BatteryState.Charging);
            return Long.MAX_VALUE;
        }

        this.setBatteryState(BatteryState.Idle);
        return Long.MAX_VALUE;
    }

}
