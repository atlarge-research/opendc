/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.simulator.compute.cpu;

import org.opendc.simulator.compute.machine.PerformanceCounters;
import org.opendc.simulator.compute.models.CpuModel;
import org.opendc.simulator.engine.FlowConsumer;
import org.opendc.simulator.engine.FlowEdge;
import org.opendc.simulator.engine.FlowGraph;
import org.opendc.simulator.engine.FlowNode;
import org.opendc.simulator.engine.FlowSupplier;

/**
 * A {@link SimCpu} of a machine.
 */
public final class SimCpu extends FlowNode implements FlowSupplier, FlowConsumer {
    private final CpuModel cpuModel;

    private final CpuPowerModel cpuPowerModel;

    private double currentCpuDemand = 0.0f; // cpu capacity demanded by the mux
    private double currentCpuUtilization = 0.0f;
    private double currentPowerDemand = 0.0f; // power demanded of the psu
    private double currentCpuSupplied = 0.0f; // cpu capacity supplied to the mux
    private double currentPowerSupplied = 0.0f; // cpu capacity supplied by the psu

    private double maxCapacity;

    private final PerformanceCounters performanceCounters = new PerformanceCounters();
    private long lastCounterUpdate;
    private final double cpuFrequencyInv;

    private FlowEdge muxEdge;
    private FlowEdge psuEdge;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Basic Getters and Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public double getFrequency() {
        return cpuModel.getTotalCapacity();
    }

    public void setFrequency(double frequency) {
        // Clamp the capacity of the CPU between [0.0, maxFreq]
        frequency = Math.max(0, Math.min(this.maxCapacity, frequency));
        //        psu.setCpuFrequency(muxInPort, frequency);
    }

    @Override
    public double getCapacity() {
        return maxCapacity;
    }

    public PerformanceCounters getPerformanceCounters() {
        return performanceCounters;
    }

    public double getPowerDraw() {
        return this.currentPowerSupplied;
    }

    public double getDemand() {
        return this.currentCpuDemand;
    }

    public double getSpeed() {
        return this.currentCpuSupplied;
    }

    public CpuModel getCpuModel() {
        return cpuModel;
    }

    @Override
    public String toString() {
        return "SimBareMetalMachine.Cpu[model=" + cpuModel + "]";
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public SimCpu(FlowGraph graph, CpuModel cpuModel, CpuPowerModel powerModel, int id) {
        super(graph);
        this.cpuModel = cpuModel;
        this.maxCapacity = this.cpuModel.getTotalCapacity();

        // TODO: connect this to the front-end
        this.cpuPowerModel = powerModel;

        this.lastCounterUpdate = graph.getEngine().getClock().millis();

        this.cpuFrequencyInv = 1 / this.maxCapacity;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FlowNode related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public long onUpdate(long now) {
        updateCounters(now);

        this.currentCpuUtilization = this.currentCpuDemand / this.maxCapacity;

        // Calculate Power Demand and send to PSU
        double powerDemand = this.cpuPowerModel.computePower(this.currentCpuUtilization);

        if (powerDemand != this.currentPowerDemand) {
            this.pushDemand(this.psuEdge, powerDemand);
        }

        // Calculate the amount of cpu this can provide
        double cpuSupply = this.currentCpuDemand;

        if (cpuSupply != this.currentCpuSupplied) {
            this.pushSupply(this.muxEdge, cpuSupply);
        }

        return Long.MAX_VALUE;
    }

    public void updateCounters() {
        this.updateCounters(this.clock.millis());
    }

    /**
     * Update the performance counters of the CPU.
     *
     * @param now The timestamp at which to update the counter.
     */
    public void updateCounters(long now) {
        long lastUpdate = this.lastCounterUpdate;
        this.lastCounterUpdate = now;
        long delta = now - lastUpdate;

        if (delta > 0) {
            double demand = this.currentCpuDemand;
            double rate = this.currentCpuSupplied;
            double capacity = this.maxCapacity;

            final double factor = this.cpuFrequencyInv * delta;

            this.performanceCounters.addCpuActiveTime(Math.round(rate * factor));
            this.performanceCounters.addCpuIdleTime(Math.round((capacity - rate) * factor));
            this.performanceCounters.addCpuStealTime(Math.round((demand - rate) * factor));
        }

        this.performanceCounters.setCpuDemand(this.currentCpuDemand);
        this.performanceCounters.setCpuSupply(this.currentCpuSupplied);
        this.performanceCounters.setCpuCapacity(this.maxCapacity);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FlowGraph Related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Push new demand to the psu
     */
    @Override
    public void pushDemand(FlowEdge supplierEdge, double newPowerDemand) {
        updateCounters();
        this.currentPowerDemand = newPowerDemand;
        this.psuEdge.pushDemand(newPowerDemand);
    }

    /**
     * Push updated supply to the mux
     */
    @Override
    public void pushSupply(FlowEdge consumerEdge, double newCpuSupply) {
        updateCounters();
        this.currentCpuSupplied = newCpuSupply;
        this.muxEdge.pushSupply(newCpuSupply);
    }

    /**
     * Handle new demand coming in from the mux
     */
    @Override
    public void handleDemand(FlowEdge consumerEdge, double newCpuDemand) {
        updateCounters();
        this.currentCpuDemand = newCpuDemand;
        this.currentCpuUtilization = this.currentCpuDemand / this.maxCapacity;

        // Calculate Power Demand and send to PSU
        double powerDemand = this.cpuPowerModel.computePower(this.currentCpuUtilization);

        if (powerDemand != this.currentPowerDemand) {
            this.pushDemand(this.psuEdge, powerDemand);
        }
    }

    /**
     * Handle updated supply from the psu
     */
    @Override
    public void handleSupply(FlowEdge supplierEdge, double newPowerSupply) {
        // TODO: Implement this
        updateCounters();
        this.currentPowerSupplied = newPowerSupply;

        // Calculate the amount of cpu this can provide
        double cpuSupply = this.currentCpuDemand;

        if (cpuSupply != this.currentCpuSupplied) {
            this.pushSupply(this.muxEdge, cpuSupply);
        }
    }

    /**
     * Add a connection to the mux
     */
    @Override
    public void addConsumerEdge(FlowEdge consumerEdge) {
        this.muxEdge = consumerEdge;
    }

    /**
     * Add a connection to the psu
     */
    @Override
    public void addSupplierEdge(FlowEdge supplierEdge) {
        this.psuEdge = supplierEdge;

        this.invalidate();
    }

    /**
     * Remove the connection to the mux
     */
    @Override
    public void removeConsumerEdge(FlowEdge consumerEdge) {
        this.muxEdge = null;
        this.invalidate();
    }

    /**
     * Remove the connection to the psu
     */
    @Override
    public void removeSupplierEdge(FlowEdge supplierEdge) {
        this.psuEdge = null;
        this.invalidate();
    }
}
