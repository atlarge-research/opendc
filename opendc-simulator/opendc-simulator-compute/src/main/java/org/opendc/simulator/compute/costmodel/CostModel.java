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

package org.opendc.simulator.compute.costmodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opendc.simulator.compute.cpu.SimCpu;
import org.opendc.simulator.compute.power.SimPowerSource;
import org.opendc.simulator.compute.machine.SimMachine;
import org.opendc.simulator.engine.engine.FlowEngine;
import org.opendc.simulator.engine.graph.FlowEdge;
import org.opendc.simulator.engine.graph.FlowNode;

/**
 * CostModel used work out the costs based on other consumers
 */
@SuppressWarnings({"unused", "FieldMayBeFinal", "FieldCanBeLocal"}) // STFU WHILE TYPE THANK YOU
public class CostModel extends FlowNode implements PowerReceiver, HostReceiver {
    private static final Logger log = LogManager.getLogger(CostModel.class); // implements PowerReceiver

    private final String hostName;
    private final long startTime; // The absolute timestamp on which the workload started
    private long lastUpdate;

    private double energyCostPerKWH = 0f;
    private double energyConsumedAccounted = 0f;
    private double energyConsumed = 0f;
    private double energyCost = 0f;

    private double employeeCost = 0f;
    private double generalCost = 0f;

    private double monthlySalaries;
    private double generalUtilities;
    private double totalHardwareValue =  0f;
    private double initialHardwareValue = 0f;

    private final List<EnergyCostFragment> fragments;
    private int fragment_index;
    private EnergyCostFragment current_fragment;

    private SimPowerSource simPowerSource = null;
    private List<SimMachine> simMachines = new ArrayList<>();


    public CostModel(FlowEngine engine, List<EnergyCostFragment> energyCostFragmentsList, long startTime, double monthlySalaries, double generalUtilities, String hostName) {
        super(engine);

        this.hostName = hostName;
        this.startTime = startTime;
        this.fragments = energyCostFragmentsList;
        this.lastUpdate = this.clock.millis();

        this.monthlySalaries = monthlySalaries;
        this.generalUtilities = generalUtilities;

        this.fragment_index = 0;
        this.current_fragment = this.fragments.get(this.fragment_index);
    }

    public void close() {
        if (this.simPowerSource != null) {
            this.simPowerSource.close();
        }

        this.closeNode();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Basic Getters and Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public String getHostName() { return this.hostName; }

    public double getEnergyCostPerKWH() {
        return energyCostPerKWH;
    }

    public double getEnergyCost() {
        return this.energyCost;
    }

    public double getEmployeeCost() {
        return this.employeeCost;
    }

    public double getGeneralCost() {
        return this.generalCost;
    }

    public double getTotalHardwareValue() {
        return this.totalHardwareValue;
    }

    public double getHardwareDegradationCost() {
        return this.initialHardwareValue - this.totalHardwareValue;
    }

    @Override
    public long onUpdate(long now) {
        long absolute_time = getAbsoluteTime(now);

        updateEnergyCost();

        if ((absolute_time < current_fragment.getStartTime()) || (absolute_time >= current_fragment.getEndTime())) {
            this.findCorrectFragment(absolute_time);
            updateEnergyCostPerKWH(current_fragment.getEnergyPrice());
        }

        return getRelativeTime(current_fragment.getEndTime());
    }

    public void updateCounters() {
        updateCounters(clock.millis());
    }

    public void updateCounters(long now) {
        /*
         * TODO we can update counters here based on time elapsed,
         * say we have X cost per Month, we can work out the elapsed time, and use
         * that to get the correct fraction of that cost relative to the elapsed time.
         */
        long lastUpdate = this.lastUpdate;
        this.lastUpdate = now;

        updateHardwareValue(now);

        long passedTime = now - lastUpdate;
        if (passedTime > 0) {
            simPowerSource.updateCounters(now);
            updateEnergyCost();
            updateStaticCosts(passedTime);
        }
    }

    private void updateHardwareValue(long now) {
        double valueCounter = 0;
        for (SimMachine machine : simMachines) {
            SimCpu cpu = machine.getCpu();
            cpu.updateCounters(now);
            valueCounter += cpu.getCurrentValue();
        }
        this.totalHardwareValue = valueCounter;

        if (this.initialHardwareValue == 0f){
            this.initialHardwareValue = valueCounter;
        }
    }

    private void updateStaticCosts(long passedTime) {
        double MillisInMonth = 365.0/12.0 * 24 * 60 * 60 * 1000.0;
        double salariesPerFragmentDuration = monthlySalaries / (MillisInMonth / passedTime);
        double generalCostsFragmentDuration =  generalUtilities / (MillisInMonth / passedTime);

        this.employeeCost += salariesPerFragmentDuration;
        this.generalCost += generalCostsFragmentDuration;
    }

    private void updateEnergyCost() {
        if (this.energyConsumed > this.energyConsumedAccounted) {
            double energyConsumedUnAccounted = this.energyConsumed - this.energyConsumedAccounted;
            this.energyCost += energyConsumedUnAccounted * this.energyCostPerKWH / 3600000;
            this.energyConsumedAccounted += energyConsumedUnAccounted;
        }
    }

    private void findCorrectFragment(long absoluteTime) {

        // Traverse to the previous fragment, until you reach the correct fragment
        while (absoluteTime < this.current_fragment.getStartTime()) {
            this.current_fragment = fragments.get(--this.fragment_index);
        }

        // Traverse to the next fragment, until you reach the correct fragment
        while (absoluteTime >= this.current_fragment.getEndTime()) {
            this.current_fragment = fragments.get(++this.fragment_index);
        }
    }

    public void updateEnergyCostPerKWH(double energyPricePerKWH) {
        this.updateCounters();
        this.energyCostPerKWH = energyPricePerKWH;
    }

    /**
     * Convert the given relative time to the absolute time by adding the start of workload
     */
    private long getAbsoluteTime(long time) {
        return time + startTime;
    }

    /**
     * Convert the given absolute time to the relative time by subtracting the start of workload
     */
    private long getRelativeTime(long time) {
        return time - startTime;
    }

    /*
     * TODO NICO Not sure how this shits work, little documention on src code but this function is required by our super
     */
    @Override
    public Map<FlowEdge.NodeType, List<FlowEdge>> getConnectedEdges() {
        return Map.of();
    }

    @Override
    public void updatePowerData(double energyConsumed) {
        this.energyConsumed = energyConsumed;
    }

    @Override
    public void setPowerSource(SimPowerSource powerSource) {
        this.simPowerSource = powerSource;
    }

    @Override
    public void removePowerSource(SimPowerSource powerSource) {
        this.simPowerSource = null;
    }

    @Override
    public void setMachine(SimMachine simMachine) {
        this.simMachines.add(simMachine);
    }

    @Override
    public void removeMachine(SimMachine simMachine) {
        this.simMachines.remove(simMachine);
    }
}
