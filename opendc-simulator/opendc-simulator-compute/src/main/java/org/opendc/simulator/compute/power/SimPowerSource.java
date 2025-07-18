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

package org.opendc.simulator.compute.power;

import java.util.List;
import java.util.Map;
import org.opendc.common.ResourceType;
import org.opendc.simulator.compute.cpu.SimCpu;
import org.opendc.simulator.engine.engine.FlowEngine;
import org.opendc.simulator.engine.graph.FlowEdge;
import org.opendc.simulator.engine.graph.FlowNode;
import org.opendc.simulator.engine.graph.FlowSupplier;

/**
 * A {@link SimPsu} implementation that estimates the power consumption based on CPU usage.
 */
public final class SimPowerSource extends FlowNode implements FlowSupplier, CarbonReceiver {
    private long lastUpdate;

    private double powerDemand = 0.0f;
    private double powerSupplied = 0.0f;
    private double totalEnergyUsage = 0.0f;

    private double carbonIntensity = 0.0f;
    private double totalCarbonEmission = 0.0f;

    private FlowEdge distributorEdge;

    private final double capacity;

    private CarbonModel carbonModel = null;

    private final String name;
    private final String clusterName;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Basic Getters and Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Determine whether the InPort is connected to a {@link SimCpu}.
     *
     * @return <code>true</code> if the InPort is connected to an OutPort, <code>false</code> otherwise.
     */
    public boolean isConnected() {
        return distributorEdge != null;
    }

    /**
     * Return the power demand of the machine (in W) measured in the PSU.
     * <p>
     * This method provides access to the power consumption of the machine before PSU losses are applied.
     */
    public double getPowerDemand() {
        return this.powerDemand;
    }

    /**
     * Return the instantaneous power usage of the machine (in W) measured at the InPort of the power supply.
     */
    public double getPowerDraw() {
        return this.powerSupplied;
    }

    public double getCarbonIntensity() {
        return this.carbonIntensity;
    }

    /**
     * Return the cumulated energy usage of the machine (in J) measured at the InPort of the powers supply.
     */
    public double getEnergyUsage() {
        return totalEnergyUsage;
    }

    public double getCarbonEmission() {
        return this.totalCarbonEmission;
    }

    @Override
    public double getCapacity() {
        return this.capacity;
    }

    public String getName() {
        return name;
    }

    public String getClusterName() {
        return clusterName;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public SimPowerSource(FlowEngine engine, double max_capacity, String name, String clusterName) {
        super(engine);

        this.capacity = max_capacity;

        lastUpdate = this.clock.millis();

        this.name = name;
        this.clusterName = clusterName;
    }

    public void close() {
        if (this.carbonModel != null) {
            this.carbonModel.close();
        }

        this.closeNode();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FlowNode related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public long onUpdate(long now) {
        return Long.MAX_VALUE;
    }

    public void updateCounters() {
        updateCounters(clock.millis());
    }

    /**
     * Calculate the energy usage up until <code>now</code>.
     */
    public void updateCounters(long now) {
        long lastUpdate = this.lastUpdate;
        this.lastUpdate = now;

        long passedTime = now - lastUpdate;
        if (passedTime > 0) {
            double energyUsage = (this.powerSupplied * passedTime * 0.001);

            // Compute the energy usage of the machine
            this.totalEnergyUsage += energyUsage;
            this.totalCarbonEmission += this.carbonIntensity * (energyUsage / 3600000.0);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FlowGraph Related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void handleIncomingDemand(FlowEdge consumerEdge, double newPowerDemand) {
        this.powerDemand = newPowerDemand;

        double powerSupply = this.powerDemand;

        if (powerSupply != this.powerSupplied) {
            this.pushOutgoingSupply(this.distributorEdge, powerSupply);
        }
    }

    @Override
    public void pushOutgoingSupply(FlowEdge consumerEdge, double newSupply) {
        updateCounters();

        this.powerSupplied = newSupply;
        consumerEdge.pushSupply(newSupply);
    }

    @Override
    public void addConsumerEdge(FlowEdge consumerEdge) {
        this.distributorEdge = consumerEdge;
    }

    @Override
    public void removeConsumerEdge(FlowEdge consumerEdge) {
        this.distributorEdge = null;
    }

    // Update the carbon intensity of the power source
    public void updateCarbonIntensity(double carbonIntensity) {
        this.updateCounters();
        this.carbonIntensity = carbonIntensity;
    }

    @Override
    public void setCarbonModel(CarbonModel carbonModel) {
        this.carbonModel = carbonModel;
    }

    @Override
    public void removeCarbonModel(CarbonModel carbonModel) {
        this.carbonModel = null;
    }

    @Override
    public Map<FlowEdge.NodeType, List<FlowEdge>> getConnectedEdges() {
        List<FlowEdge> supplierEdges = this.distributorEdge != null ? List.of(this.distributorEdge) : List.of();

        return Map.of(FlowEdge.NodeType.SUPPLYING, supplierEdges);
    }

    @Override
    public ResourceType getSupplierResourceType() {
        return ResourceType.POWER;
    }
}
