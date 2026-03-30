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

import java.util.LinkedList;
import org.opendc.simulator.compute.power.batteries.BatteryAggregator;
import org.opendc.simulator.compute.power.batteries.BatteryState;
import org.opendc.simulator.compute.power.batteries.SimBattery;
import org.opendc.simulator.engine.engine.FlowEngine;

/**
 * An improved version of {@link RunningMeanBatteryPolicy}.
 * It uses the same logic, but only start charging if the carbon intensity is not decreasing anymore.
 */
public class RunningMeanPlusBatteryPolicy extends BatteryPolicy {
    private final int windowSize;

    private final LinkedList<Double> pastCarbonIntensities = new LinkedList<>();
    private double previousCarbonIntensity = 0.0;

    private double pastCarbonIntensitiesSum = 0.0;
    private double pastCarbonIntensitiesMean = 0.0;

    /**
     *
     * @param engine     The {@link FlowEngine} this stage belongs to.
     * @param battery        The {@link SimBattery} to control.
     * @param aggregator    The {@link BatteryAggregator} to use.
     */
    public RunningMeanPlusBatteryPolicy(
            FlowEngine engine,
            SimBattery battery,
            BatteryAggregator aggregator,
            double startingThreshold,
            int windowSize) {
        super(engine, battery, aggregator);

        this.windowSize = windowSize;

        this.updatePastCarbonIntensities(startingThreshold);
    }

    private void updatePastCarbonIntensities(double newCarbonIntensity) {
        if (this.pastCarbonIntensities.size() == this.windowSize) {
            this.pastCarbonIntensitiesSum -= this.pastCarbonIntensities.removeFirst();
        }

        if (this.pastCarbonIntensities.size() > 0) {
            this.previousCarbonIntensity = this.pastCarbonIntensities.getLast();
        }

        this.pastCarbonIntensities.addLast(newCarbonIntensity);
        this.pastCarbonIntensitiesSum += newCarbonIntensity;
        this.pastCarbonIntensitiesMean = this.pastCarbonIntensitiesSum / this.pastCarbonIntensities.size();
    }

    @Override
    public void updateCarbonIntensity(double newCarbonIntensity) {
        this.updatePastCarbonIntensities(newCarbonIntensity);

        super.updateCarbonIntensity(newCarbonIntensity);
    }

    private boolean isCharging = false;

    @Override
    public long onUpdate(long now) {
        if (this.carbonIntensity >= this.pastCarbonIntensitiesMean & !this.battery.isEmpty()) {
            this.isCharging = false;
            this.setBatteryState(BatteryState.DISCHARGING);
            return Long.MAX_VALUE;
        }

        if (this.carbonIntensity < this.pastCarbonIntensitiesMean & !this.battery.isFull()) {
            if (this.carbonIntensity >= this.previousCarbonIntensity || this.isCharging) {
                this.setBatteryState(BatteryState.CHARGING);
                return Long.MAX_VALUE;
            }
        }

        this.isCharging = false;
        this.setBatteryState(BatteryState.IDLE);
        return Long.MAX_VALUE;
    }
}
