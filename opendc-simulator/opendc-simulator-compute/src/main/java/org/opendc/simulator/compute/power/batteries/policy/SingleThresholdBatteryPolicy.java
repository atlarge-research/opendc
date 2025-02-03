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

import org.opendc.simulator.compute.power.batteries.BatteryAggregator;
import org.opendc.simulator.compute.power.batteries.BatteryState;
import org.opendc.simulator.compute.power.batteries.SimBattery;
import org.opendc.simulator.engine.graph.FlowGraph;

public class SingleThresholdBatteryPolicy extends BatteryPolicy {
    private final double carbonThreshold;

    /**
     *
     * @param parentGraph     The {@link FlowGraph} this stage belongs to.
     * @param battery
     * @param aggregator
     * @param carbonThreshold
     */
    public SingleThresholdBatteryPolicy(
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
