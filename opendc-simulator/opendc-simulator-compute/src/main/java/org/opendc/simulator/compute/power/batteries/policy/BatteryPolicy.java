/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.simulator.compute.power.batteries.policy;

import org.opendc.simulator.compute.power.CarbonModel;
import org.opendc.simulator.compute.power.CarbonReceiver;
import org.opendc.simulator.compute.power.batteries.BatteryAggregator;
import org.opendc.simulator.compute.power.batteries.BatteryState;
import org.opendc.simulator.compute.power.batteries.PowerSourceType;
import org.opendc.simulator.compute.power.batteries.SimBattery;
import org.opendc.simulator.engine.graph.FlowGraph;
import org.opendc.simulator.engine.graph.FlowNode;

public abstract class BatteryPolicy extends FlowNode implements CarbonReceiver {

    protected final SimBattery battery;
    protected final BatteryAggregator aggregator;

    protected double carbonIntensity;

    protected BatteryState batteryState = BatteryState.IDLE;

    /**
     * Construct a new {@link FlowNode} instance.
     *
     * @param parentGraph The {@link FlowGraph} this stage belongs to.
     */
    public BatteryPolicy(FlowGraph parentGraph, SimBattery battery, BatteryAggregator aggregator) {
        super(parentGraph);

        this.battery = battery;
        this.battery.setBatteryPolicy(this);

        this.aggregator = aggregator;
    }

    public void close() {
        this.closeNode();
    }

    @Override
    public abstract long onUpdate(long now);

    public void setBatteryState(BatteryState newBatteryState) {
        if (newBatteryState == this.batteryState) {
            return;
        }

        this.batteryState = newBatteryState;

        if (newBatteryState == BatteryState.CHARGING) {
            this.battery.setBatteryState(BatteryState.CHARGING);
            this.aggregator.setPowerSourceType(PowerSourceType.PowerSource);
            return;
        }

        if (newBatteryState == BatteryState.IDLE) {
            this.battery.setBatteryState(BatteryState.IDLE);
            this.aggregator.setPowerSourceType(PowerSourceType.PowerSource);
            return;
        }

        if (newBatteryState == BatteryState.DISCHARGING) {
            this.battery.setBatteryState(BatteryState.DISCHARGING);
            this.aggregator.setPowerSourceType(PowerSourceType.Battery);
        }
    }

    @Override
    public void updateCarbonIntensity(double newCarbonIntensity) {
        if (newCarbonIntensity == this.carbonIntensity) {
            return;
        }

        this.carbonIntensity = newCarbonIntensity;

        this.invalidate();
    }

    @Override
    public void setCarbonModel(CarbonModel carbonModel) {}

    @Override
    public void removeCarbonModel(CarbonModel carbonModel) {
        this.close();
    }
}
