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

import java.util.List;
import java.util.Map;

import org.opendc.common.ResourceType;
import org.opendc.simulator.compute.machine.CpuPerformanceCounters;
import org.opendc.simulator.compute.models.CpuModel;
import org.opendc.simulator.engine.engine.FlowEngine;
import org.opendc.simulator.engine.graph.FlowConsumer;
import org.opendc.simulator.engine.graph.FlowEdge;
import org.opendc.simulator.engine.graph.FlowNode;
import org.opendc.simulator.engine.graph.FlowSupplier;

/**
 * A {@link SimCpu} of a machine.
 */
public final class SimCpu extends FlowNode implements FlowSupplier, FlowConsumer {
    private final CpuModel cpuModel;

    private final CpuPowerModel cpuPowerModel;

    private double currentCpuDemand = 0.0f; // cpu capacity demanded by the mux
    private double currentCpuUtilization = 0.0f;
    private double currentCpuSupplied = 0.0f; // cpu capacity supplied to the mux

    private double currentPowerDemand; // power demanded of the psu
    private double currentPowerSupplied = 0.0f; // cpu capacity supplied by the psu

    private double maxCapacity;

    private final CpuPerformanceCounters cpuPerformanceCounters = new CpuPerformanceCounters();
    private long lastCounterUpdate;
    private final double cpuFrequencyInv;

    private FlowEdge distributorEdge;
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

    public CpuPerformanceCounters getPerformanceCounters() {
        return cpuPerformanceCounters;
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

    public SimCpu(FlowEngine engine, CpuModel cpuModel, CpuPowerModel powerModel, int id) {
        super(engine);
        this.cpuModel = cpuModel;
        this.maxCapacity = this.cpuModel.getTotalCapacity();

        this.cpuPowerModel = powerModel;

        this.lastCounterUpdate = clock.millis();

        this.cpuFrequencyInv = 1 / this.maxCapacity;

        this.currentPowerDemand = this.cpuPowerModel.computePower(this.currentCpuUtilization);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FlowNode related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public long onUpdate(long now) {
        updateCounters(now);

        // Check if supply == demand
        if (this.currentPowerDemand != this.currentPowerSupplied) {
            this.pushOutgoingDemand(this.psuEdge, this.currentPowerDemand);

            return Long.MAX_VALUE;
        }

        this.currentCpuSupplied = Math.min(this.currentCpuDemand, this.maxCapacity);

        this.pushOutgoingSupply(this.distributorEdge, this.currentCpuSupplied, ResourceType.CPU);

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

            this.cpuPerformanceCounters.addCpuActiveTime(Math.round(rate * factor));
            this.cpuPerformanceCounters.addCpuIdleTime(Math.round((capacity - rate) * factor));
            this.cpuPerformanceCounters.addCpuStealTime(Math.round((demand - rate) * factor));
        }

        this.cpuPerformanceCounters.setCpuDemand(this.currentCpuDemand);
        this.cpuPerformanceCounters.setCpuSupply(this.currentCpuSupplied);
        this.cpuPerformanceCounters.setCpuCapacity(this.maxCapacity);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FlowGraph Related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Push new demand to the psu
     */
    @Override
    public void pushOutgoingDemand(FlowEdge supplierEdge, double newPowerDemand) {
        updateCounters();
        this.currentPowerDemand = newPowerDemand;
        this.psuEdge.pushDemand(newPowerDemand);
    }

    /**
     * Push updated supply to the mux
     */
    @Override
    public void pushOutgoingSupply(FlowEdge consumerEdge, double newCpuSupply) {
        updateCounters();
        this.currentCpuSupplied = newCpuSupply;

        this.distributorEdge.pushSupply(newCpuSupply, true, ResourceType.CPU);
    }

    @Override
    public void pushOutgoingSupply(FlowEdge consumerEdge, double newCpuSupply, ResourceType resourceType) {
        updateCounters();
        this.currentCpuSupplied = newCpuSupply;

        this.distributorEdge.pushSupply(newCpuSupply, true, resourceType);
    }

    /**
     * Handle new demand coming in from the mux
     */
    @Override
    public void handleIncomingDemand(FlowEdge consumerEdge, double newCpuDemand) {
        updateCounters();
        this.currentCpuDemand = newCpuDemand;

        this.currentCpuUtilization = Math.min(this.currentCpuDemand / this.maxCapacity, 1.0);

        // Calculate Power Demand and send to PSU
        this.currentPowerDemand = this.cpuPowerModel.computePower(this.currentCpuUtilization);

        this.invalidate();
    }

    /**
     * Handle updated supply from the psu
     */
    @Override
    public void handleIncomingSupply(FlowEdge supplierEdge, double newPowerSupply) {
        updateCounters();
        this.currentPowerSupplied = newPowerSupply;

        this.invalidate();
    }

    /**
     * Add a connection to the mux
     */
    @Override
    public void addConsumerEdge(FlowEdge consumerEdge) {
        this.distributorEdge = consumerEdge;
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
        this.distributorEdge = null;
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

    @Override
    public Map<FlowEdge.NodeType, List<FlowEdge>> getConnectedEdges() {
        return Map.of(
                FlowEdge.NodeType.CONSUMING, List.of(this.psuEdge),
                FlowEdge.NodeType.SUPPLYING, List.of(this.distributorEdge));
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.CPU;
    }
}
