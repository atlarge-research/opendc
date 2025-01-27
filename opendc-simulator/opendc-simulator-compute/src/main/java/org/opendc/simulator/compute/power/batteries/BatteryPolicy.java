package org.opendc.simulator.compute.power.batteries;

import org.opendc.simulator.compute.power.CarbonModel;
import org.opendc.simulator.compute.power.CarbonReceiver;
import org.opendc.simulator.engine.graph.FlowGraph;
import org.opendc.simulator.engine.graph.FlowNode;

public class BatteryPolicy extends FlowNode implements CarbonReceiver {

    private final SimBattery battery;
    private final BatteryAggregator aggregator;


    private final double carbonThreshold;
    private double carbonIntensity;

    /**
     * Construct a new {@link FlowNode} instance.
     *
     * @param parentGraph The {@link FlowGraph} this stage belongs to.
     */
    public BatteryPolicy(FlowGraph parentGraph, SimBattery battery, BatteryAggregator aggregator, double carbonThreshold) {
        super(parentGraph);

        this.carbonThreshold = carbonThreshold;

        this.battery = battery;
        this.battery.setBatteryPolicy(this);

        this.aggregator = aggregator;
    }

    public void close() {
        this.closeNode();
    }

    @Override
    public long onUpdate(long now) {

        if (this.carbonIntensity >= this.carbonThreshold & !this.battery.isEmpty()) {
            this.battery.setBatteryState(BatteryState.Discharging);
            this.aggregator.setPowerSourceType(PowerSourceType.Battery);
            return Long.MAX_VALUE;
        }

        if (this.carbonIntensity < this.carbonThreshold & !this.battery.isFull()) {
            this.battery.setBatteryState(BatteryState.Charging);
            this.aggregator.setPowerSourceType(PowerSourceType.PowerSource);
            return Long.MAX_VALUE;
        }


        this.aggregator.setPowerSourceType(PowerSourceType.PowerSource);
        this.battery.setBatteryState(BatteryState.Idle);
        return Long.MAX_VALUE;
    }

    @Override
    public void updateCarbonIntensity(double carbonIntensity) {
        this.carbonIntensity = carbonIntensity;

        this.invalidate();
    }

    @Override
    public void setCarbonModel(CarbonModel carbonModel) {}

    @Override
    public void removeCarbonModel(CarbonModel carbonModel) {
        this.close();
    }
}
