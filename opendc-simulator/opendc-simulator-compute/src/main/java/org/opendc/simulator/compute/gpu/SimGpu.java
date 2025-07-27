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

package org.opendc.simulator.compute.gpu;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.opendc.common.ResourceType;
import org.opendc.simulator.compute.ComputeResource;
import org.opendc.simulator.compute.machine.PerformanceCounters;
import org.opendc.simulator.compute.models.GpuModel;
import org.opendc.simulator.compute.power.PowerModel;
import org.opendc.simulator.compute.virtualization.VirtualizationOverheadModel;
import org.opendc.simulator.engine.engine.FlowEngine;
import org.opendc.simulator.engine.graph.FlowConsumer;
import org.opendc.simulator.engine.graph.FlowEdge;
import org.opendc.simulator.engine.graph.FlowNode;
import org.opendc.simulator.engine.graph.FlowSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link SimGpu} of a machine.
 */
public final class SimGpu extends FlowNode implements FlowSupplier, FlowConsumer, ComputeResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimGpu.class);
    private final int id;
    private final GpuModel gpuModel;

    private final PowerModel gpuPowerModel;

    private double currentGpuDemand = 0.0f; // gpu capacity demanded by the mux
    private double currentGpuUtilization = 0.0f;
    private double currentGpuSupplied = 0.0f; // gpu capacity supplied to the mux

    private double currentPowerDemand; // power demanded of the psu
    private double currentPowerSupplied = 0.0f; // gpu capacity supplied by the psu

    private double maxCapacity;

    private final PerformanceCounters performanceCounters = new PerformanceCounters();
    private long lastCounterUpdate;
    private final double gpuFrequencyInv;

    private final VirtualizationOverheadModel virtualizationOverheadModel;
    private int consumerCount = 0; // Number of consumers connected to this GPU

    private FlowEdge distributorEdge;
    private FlowEdge psuEdge;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Basic Getters and Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public double getFrequency() {
        return gpuModel.getTotalCoreCapacity();
    }

    public int getId() {
        return id;
    }

    @Override
    public double getCapacity() {
        return maxCapacity;
    } // TODO: take memory into account

    public PerformanceCounters getPerformanceCounters() {
        return performanceCounters;
    }

    public double getPowerDraw() {
        return this.currentPowerSupplied;
    }

    public double getDemand() {
        return this.currentGpuDemand;
    }

    // TODO: take memory into account
    public double getSupply() {
        return this.currentGpuSupplied;
    } // TODO: take memory into account

    public GpuModel getGpuModel() {
        return gpuModel;
    }

    @Override
    public String toString() {
        return "SimBareMetalMachine.Gpu[model=" + gpuModel + "]";
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public SimGpu(
            FlowEngine engine,
            GpuModel gpuModel,
            PowerModel powerModel,
            int id,
            VirtualizationOverheadModel overheadModel) {
        super(engine);
        this.id = id;
        this.gpuModel = gpuModel;
        this.maxCapacity = this.gpuModel.getTotalCoreCapacity();

        this.gpuPowerModel = powerModel;

        this.lastCounterUpdate = clock.millis();

        this.gpuFrequencyInv = 1 / this.maxCapacity;

        this.currentPowerDemand = this.gpuPowerModel.computePower(this.currentGpuUtilization);
        this.virtualizationOverheadModel = overheadModel;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FlowNode related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public long onUpdate(long now) {
        updateCounters(now);

        // Check if supply == demand
        // using big decimal to avoid floating point precision issues
        if (!new BigDecimal(this.currentPowerDemand).setScale(5, RoundingMode.HALF_UP).equals(new BigDecimal(this.currentPowerSupplied).setScale(5, RoundingMode.HALF_UP))){
            this.pushOutgoingDemand(this.psuEdge, this.currentPowerDemand);

            return Long.MAX_VALUE;
        }
        this.currentGpuSupplied = virtualizationOverheadModel.getSupply(
                Math.min(this.currentGpuDemand, this.maxCapacity), this.consumerCount);
        this.pushOutgoingSupply(this.distributorEdge, this.currentGpuSupplied);

        return Long.MAX_VALUE;
    }

    public void updateCounters() {
        this.updateCounters(this.clock.millis());
    }

    /**
     * Update the performance counters of the GPU.
     *
     * @param now The timestamp at which to update the counter.
     */
    public void updateCounters(long now) {
        long lastUpdate = this.lastCounterUpdate;
        this.lastCounterUpdate = now;
        long delta = now - lastUpdate;

        if (delta > 0) {
            double demand = this.currentGpuDemand;
            double rate = this.currentGpuSupplied;
            double capacity = this.maxCapacity;

            final double factor = this.gpuFrequencyInv * delta;

            this.performanceCounters.addActiveTime(Math.round(rate * factor));
            this.performanceCounters.addIdleTime(Math.round((capacity - rate) * factor));
            this.performanceCounters.addStealTime(Math.round((demand - rate) * factor));
        }

        this.performanceCounters.setDemand(this.currentGpuDemand);
        this.performanceCounters.setSupply(this.currentGpuSupplied);
        this.performanceCounters.setCapacity(this.maxCapacity);
        this.performanceCounters.setPowerDraw(this.currentPowerSupplied);
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
        this.psuEdge.pushDemand(newPowerDemand, false, ResourceType.POWER);
    }

    /**
     * Push updated supply to the mux
     */
    @Override
    public void pushOutgoingSupply(FlowEdge consumerEdge, double newGpuSupply) {
        updateCounters();
        this.currentGpuSupplied = newGpuSupply;

        this.distributorEdge.pushSupply(newGpuSupply, true, ResourceType.POWER);
    }

    /**
     * Push updated supply to the mux
     */
    @Override
    public void pushOutgoingSupply(FlowEdge consumerEdge, double newGpuSupply, ResourceType resourceType) {
        updateCounters();
        this.currentGpuSupplied = newGpuSupply;

        this.distributorEdge.pushSupply(newGpuSupply, true, resourceType);
    }

    @Override
    public void handleIncomingDemand(FlowEdge consumerEdge, double newGpuDemand) {
        updateCounters();
        this.currentGpuDemand = newGpuDemand;

        this.currentGpuUtilization = Math.min(this.currentGpuDemand / this.maxCapacity, 1.0);

        // Calculate Power Demand and send to PSU
        this.currentPowerDemand = this.gpuPowerModel.computePower(this.currentGpuUtilization);

        this.invalidate();
    }

    /**
     * Handle new demand coming in from the mux
     */
    @Override
    public void handleIncomingDemand(
            FlowEdge consumerEdge, double newGpuDemand, ResourceType resourceType, int consumerCount) {
        if (resourceType != ResourceType.GPU) {
            throw new IllegalArgumentException("Resource type must be GPU");
        }
        updateCounters();
        this.currentGpuDemand = newGpuDemand;
        this.consumerCount = consumerCount;

        this.currentGpuUtilization = Math.min(this.currentGpuDemand / this.maxCapacity, 1.0);

        // Calculate Power Demand and send to PSU
        this.currentPowerDemand = this.gpuPowerModel.computePower(this.currentGpuUtilization);

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
    public ResourceType getSupplierResourceType() {
        return ResourceType.GPU;
    }

    @Override
    public ResourceType getConsumerResourceType() {
        return ResourceType.GPU;
    }
}
