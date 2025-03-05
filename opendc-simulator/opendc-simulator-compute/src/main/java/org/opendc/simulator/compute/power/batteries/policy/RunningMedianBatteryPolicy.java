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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

public class RunningMedianBatteryPolicy extends BatteryPolicy {
    private final LinkedList<Double> pastCarbonIntensities = new LinkedList<>();
    private final int windowSize;

    private double medianCarbonIntensity = 0.0;

    /**
     *
     * @param parentGraph     The {@link FlowGraph} this stage belongs to.
     * @param battery        The {@link SimBattery} to control.
     * @param aggregator    The {@link BatteryAggregator} to use.
     */
    public RunningMedianBatteryPolicy(
            FlowGraph parentGraph, SimBattery battery, BatteryAggregator aggregator, double startingThreshold, int windowSize) {
        super(parentGraph, battery, aggregator);

        this.windowSize = windowSize;

        this.updateCarbonIntensities(startingThreshold);
    }

    public static double getMedian(ArrayList<Double> list) {
        return getPositionalValue(list, 0.5);
    }

    public static double getPositionalValue(ArrayList<Double> list, double position) {
        if (list.size() == 1) {
            return list.get(0);
        }

        Collections.sort(list);
        int size = list.size();

        double middle_index = size * position;
        if (middle_index % 1 == 0) {
            return list.get((int) middle_index);
        } else {
            return (list.get((int) middle_index) + list.get((int) middle_index + 1)) / 2.0;
        }
    }

    private void updateCarbonIntensities(double newCarbonIntensity) {
        if (this.pastCarbonIntensities.size() == this.windowSize) {
            this.pastCarbonIntensities.removeFirst();
        }
        this.pastCarbonIntensities.addLast(newCarbonIntensity);

        ArrayList<Double> carbonIntensityList = new ArrayList<>(this.pastCarbonIntensities);
        this.medianCarbonIntensity = getMedian(carbonIntensityList);
    }

    @Override
    public void updateCarbonIntensity(double newCarbonIntensity) {
        this.updateCarbonIntensities(newCarbonIntensity);

        super.updateCarbonIntensity(newCarbonIntensity);
    }

    @Override
    public long onUpdate(long now) {
        if (this.carbonIntensity >= this.medianCarbonIntensity & !this.battery.isEmpty()) {
            this.setBatteryState(BatteryState.DISCHARGING);
            return Long.MAX_VALUE;
        }

        if (this.carbonIntensity < this.medianCarbonIntensity & !this.battery.isFull()) {
            this.setBatteryState(BatteryState.CHARGING);
            return Long.MAX_VALUE;
        }

        this.setBatteryState(BatteryState.IDLE);
        return Long.MAX_VALUE;
    }
}
