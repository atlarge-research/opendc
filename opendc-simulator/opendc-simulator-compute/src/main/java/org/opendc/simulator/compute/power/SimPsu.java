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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendc.common.ResourceType;
import org.opendc.simulator.compute.cpu.SimCpu;
import org.opendc.simulator.engine.engine.FlowEngine;
import org.opendc.simulator.engine.graph.FlowConsumer;
import org.opendc.simulator.engine.graph.FlowEdge;
import org.opendc.simulator.engine.graph.FlowNode;
import org.opendc.simulator.engine.graph.FlowSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link SimPsu} implementation that estimates the power consumption based on CPU usage.
 */
public final class SimPsu extends FlowNode implements FlowSupplier, FlowConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimPsu.class);
    private long lastUpdate;

    private final HashMap<ResourceType, HashMap<Integer, Double>> powerDemandsPerResource = new HashMap<>();
    private final HashMap<ResourceType, HashMap<Integer, Double>> powerSuppliedPerResource = new HashMap<>();

    private double totalPowerDemand = 0.0;
    private double totalPowerSupplied = 0.0;
    private double totalEnergyUsage = 0.0;

    private final HashMap<ResourceType, HashMap<Integer, FlowEdge>> resourceEdges = new HashMap<>();
    private FlowEdge powerSupplyEdge;

    private final double capacity = Long.MAX_VALUE;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Basic Getters and Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Determine whether the InPort is connected to a {@link SimCpu}.
     *
     * @return <code>true</code> if the InPort is connected to an OutPort, <code>false</code> otherwise.
     */
    public boolean isConnected() {
        return !this.resourceEdges.isEmpty()
                && this.resourceEdges.values().stream().anyMatch(list -> !list.isEmpty());
    }

    /**
     * Return the power demand of the machine (in W) measured in the PSU.
     * <p>
     * This method provides access to the power consumption of the machine before PSU losses are applied.
     */
    public double getPowerDemand() {
        return  this.totalPowerDemand;
    }

    /**
     * Return the power demand of the machine (in W) measured in the PSU for a specific resource type.
     * <p>
     * This method provides access to the power consumption of the machine before PSU losses are applied.
     */
    public double getPowerDemand(ResourceType resourceType) {
//        return this.powerDemandsPerResource.get(resourceType).stream().mapToDouble(Double::doubleValue).sum();
        return this.powerDemandsPerResource.get(resourceType).values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    /**
     * Return the power demand of the machine (in W) measured in the PSU for a specific resource type for a specific resource.
     * <p>
     * This method provides access to the power consumption of the machine before PSU losses are applied.
     */
    public double getPowerDemand(ResourceType resourceType, int id) {
        return this.powerDemandsPerResource.get(resourceType).get(id);
    }


    /**
     * Return the instantaneous power usage of the machine (in W) measured at the InPort of the power supply.
     */
    public double getPowerDraw() {
        return this.totalPowerSupplied;
    }

    /**
     * Return the instantaneous power usage of the machine (in W) measured at the InPort of the power supply for a specific resource type.
     */
    public double getPowerDraw(ResourceType resourceType) {
        return this.powerSuppliedPerResource.get(resourceType).values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    /**
     * Return the instantaneous power usage of the machine (in W) measured at the InPort of the power supply for a specific resource type for a specific resource.
     */
    public double getPowerDraw(ResourceType resourceType, int id) {
        return this.powerSuppliedPerResource.get(resourceType).get(id);
    }

    /**
     * Return the cumulated energy usage of the machine (in J) measured at the InPort of the powers supply.
     */
    public double getEnergyUsage() {
        updateCounters();
        return totalEnergyUsage;
    }

    @Override
    public double getCapacity() {
        return this.capacity;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public SimPsu(FlowEngine engine) {
        super(engine);

        lastUpdate = this.clock.millis();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FlowNode related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public long onUpdate(long now) {
        updateCounters();
        for (ResourceType resourceType : this.resourceEdges.keySet()) {
            HashMap<Integer, FlowEdge> edges = this.resourceEdges.get(resourceType);
            if (edges != null && !edges.isEmpty()) {
                for (FlowEdge edge : edges.values()) {
                    // If the edge is null, it means that the edge has been removed -> no update is needed
                    if (edge == null) {
                        continue;
                    }

                    int consumerIndex = edge.getConsumerIndex() == -1 ? 0 : edge.getConsumerIndex();
                    double powerDemand =
                        this.powerDemandsPerResource.get(resourceType).get(consumerIndex);
                    double powerSupplied =
                        this.powerSuppliedPerResource.get(resourceType).get(consumerIndex);

                    if (powerDemand != powerSupplied) {
                        edge.pushSupply(powerDemand);
                    }
                }
            }
        }

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

        long duration = now - lastUpdate;
        if (duration > 0) {
            for (ResourceType resourceType : this.powerSuppliedPerResource.keySet()) {
                for (double powerSupplied : this.powerSuppliedPerResource.get(resourceType).values()) {
                    this.totalEnergyUsage += (powerSupplied * duration * 0.001);
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FlowGraph Related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void pushOutgoingDemand(FlowEdge supplierEdge, double newDemand) {
        this.powerSupplyEdge.pushDemand(newDemand);
    }

    @Override
    public void pushOutgoingSupply(FlowEdge consumerEdge, double newSupply) {
        this.pushOutgoingSupply(consumerEdge, newSupply, consumerEdge.getConsumerResourceType());
    }

    @Override
    public void pushOutgoingSupply(FlowEdge consumerEdge, double newSupply, ResourceType resourceType) {
        int consumerIndex = consumerEdge.getConsumerIndex() == -1 ? 0 : consumerEdge.getConsumerIndex();

        double previousSupply = this.powerSuppliedPerResource.get(resourceType).get(consumerIndex);
        this.totalPowerSupplied += newSupply - previousSupply;

        this.powerSuppliedPerResource.get(resourceType).put(consumerIndex, newSupply);

        consumerEdge.pushSupply(newSupply, false, resourceType);
    }

    @Override
    public void handleIncomingDemand(FlowEdge consumerEdge, double newDemand) {
        handleIncomingDemand(consumerEdge, newDemand, consumerEdge.getConsumerResourceType());
    }

    @Override
    public void handleIncomingDemand(FlowEdge consumerEdge, double newPowerDemand, ResourceType resourceType) {
        updateCounters();
        int consumerIndex = consumerEdge.getConsumerIndex() == -1 ? 0 : consumerEdge.getConsumerIndex();

        double previousPowerDemand =this.powerDemandsPerResource.get(resourceType).get(consumerIndex);
        this.totalPowerDemand += newPowerDemand - previousPowerDemand;

        this.powerDemandsPerResource.get(resourceType).put(consumerIndex, newPowerDemand);

        pushOutgoingDemand(this.powerSupplyEdge, totalPowerDemand);
    }

    @Override
    public void handleIncomingSupply(FlowEdge supplierEdge, double newSupply) {
        updateCounters();
        for (ResourceType resourceType : this.resourceEdges.keySet()) {
            for (FlowEdge edge : this.resourceEdges.get(resourceType).values()) {
                // If the edge is null, it means that the edge has been removed -> no update is needed
                if (edge == null) {
                    continue;
                }
                int consumerIndex = edge.getConsumerIndex() == -1 ? 0 : edge.getConsumerIndex();
                double outgoingSupply =
                        Math.min(this.powerDemandsPerResource.get(resourceType).get(consumerIndex), newSupply);
                pushOutgoingSupply(edge, outgoingSupply, resourceType);
            }
        }
    }

    @Override
    public void addConsumerEdge(FlowEdge consumerEdge) {

        ResourceType consumerResourceType = consumerEdge.getConsumerResourceType();
        int consumerIndex = consumerEdge.getConsumerIndex() == -1 ? 0 : consumerEdge.getConsumerIndex();

        if (!this.resourceEdges.containsKey(consumerResourceType)) {
            this.resourceEdges.put(consumerResourceType, new HashMap<>());
            this.powerDemandsPerResource.put(consumerResourceType, new HashMap<>());
            this.powerSuppliedPerResource.put(consumerResourceType, new HashMap<>());
        }

        this.resourceEdges.get(consumerResourceType).put(consumerIndex, consumerEdge);
        this.powerDemandsPerResource.get(consumerResourceType).put(consumerIndex, 0.0);
        this.powerSuppliedPerResource.get(consumerResourceType).put(consumerIndex, 0.0);
    }

    @Override
    public void addSupplierEdge(FlowEdge supplierEdge) {
        this.powerSupplyEdge = supplierEdge;
    }

    @Override
    public void removeConsumerEdge(FlowEdge consumerEdge) {
        ResourceType resourceType = consumerEdge.getConsumerResourceType();
        int consumerIndex = consumerEdge.getConsumerIndex() == -1 ? 0 : consumerEdge.getConsumerIndex();

        if (this.resourceEdges.containsKey(resourceType)) {
            this.resourceEdges.get(resourceType).put(consumerIndex, null);

            this.totalPowerDemand -= this.powerDemandsPerResource.get(resourceType).get(consumerIndex);
            this.powerDemandsPerResource.get(resourceType).put(consumerIndex, 0.0);

            this.totalPowerSupplied -= this.powerSuppliedPerResource.get(resourceType).get(consumerIndex);
            this.powerSuppliedPerResource.get(resourceType).put(consumerIndex, 0.0);
        }
    }

    @Override
    public void removeSupplierEdge(FlowEdge supplierEdge) {
        this.powerSupplyEdge = null;
    }

    @Override
    public Map<FlowEdge.NodeType, List<FlowEdge>> getConnectedEdges() {
        List<FlowEdge> supplyingEdges = new ArrayList<>();
        for (ResourceType resourceType : this.resourceEdges.keySet()) {
            List<FlowEdge> edges = this.resourceEdges.get(resourceType).values().stream().toList();
            if (edges != null && !edges.isEmpty()) {
                supplyingEdges.addAll(edges);
            }
        }
        List<FlowEdge> consumingEdges = powerSupplyEdge != null ? List.of(powerSupplyEdge) : List.of();

        return Map.of(
                FlowEdge.NodeType.SUPPLYING, supplyingEdges,
                FlowEdge.NodeType.CONSUMING, consumingEdges);
    }

    @Override
    public ResourceType getSupplierResourceType() {
        return ResourceType.POWER;
    }

    @Override
    public ResourceType getConsumerResourceType() {
        return ResourceType.POWER;
    }
}
