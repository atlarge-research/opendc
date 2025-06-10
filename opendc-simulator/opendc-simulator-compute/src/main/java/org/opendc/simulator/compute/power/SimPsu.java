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

import jdk.jshell.spi.ExecutionControl;
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

    private final HashMap<ResourceType, ArrayList<Double>> powerDemandsPerResource = new HashMap<>();
    private final HashMap<ResourceType, ArrayList<Double>> powerSuppliedPerResource = new HashMap<>();
    private double totalEnergyUsage = 0.0;

    private final HashMap<ResourceType, ArrayList<FlowEdge>> resourceEdges = new HashMap<>();
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
            return !this.resourceEdges.isEmpty() &&
                   this.resourceEdges.values().stream().anyMatch(list -> !list.isEmpty());
    }

    /**
     * Return the power demand of the machine (in W) measured in the PSU.
     * <p>
     * This method provides access to the power consumption of the machine before PSU losses are applied.
     */
    public double getPowerDemand() {
        return this.powerDemandsPerResource.values().stream()
                .flatMap(List::stream)
                .findFirst()
                .orElse(0.0);
    }

    public double getPowerDemand(ResourceType resourceType) {
        return this.powerDemandsPerResource.get(resourceType).getFirst();
    }

    /**
     * Return the instantaneous power usage of the machine (in W) measured at the InPort of the power supply.
     */
    public double getPowerDraw() {
        return this.powerSuppliedPerResource.values().stream()
                .flatMap(List::stream)
                .findFirst()
                .orElse(0.0);
    }
    public double getPowerDraw(ResourceType resourceType) {
        return this.powerSuppliedPerResource.get(resourceType).getFirst();
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
            ArrayList<FlowEdge> edges = this.resourceEdges.get(resourceType);
            if (edges != null && !edges.isEmpty()) {
                double powerSupply = this.powerDemandsPerResource.get(resourceType).getFirst();
                double powerSupplied = this.powerSuppliedPerResource.get(resourceType).getFirst();

                if (powerSupply != powerSupplied) {
                    for (FlowEdge edge : edges) {
                        edge.pushSupply(powerSupply);
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
                for (double powerSupplied : this.powerSuppliedPerResource.get(resourceType)) {
                    this.totalEnergyUsage += (powerSupplied * duration * 0.001);
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // FlowGraph Related functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void pushOutgoingDemand(FlowEdge supplierEdge, double newDemand, ResourceType resourceType) {
        this.powerDemandsPerResource.put(resourceType, new ArrayList<>(List.of(newDemand)));
        powerSupplyEdge.pushDemand(newDemand);
    }

    @Override
    public void pushOutgoingDemand(FlowEdge supplierEdge, double newDemand) {
        double totalDemand = this.powerDemandsPerResource.values().stream()
                .flatMap(List::stream)
                .reduce(0.0, Double::sum);
        this.powerSupplyEdge.pushDemand(totalDemand);
    }

    @Override
    public void pushOutgoingSupply(FlowEdge consumerEdge, double newSupply) {
        this.pushOutgoingSupply(consumerEdge, newSupply, consumerEdge.getConsumer().getResourceType());
    }

    @Override
    public void pushOutgoingSupply(FlowEdge consumerEdge, double newSupply, ResourceType resourceType) {
        this.powerSuppliedPerResource.put(resourceType, new ArrayList<>(List.of(newSupply)));
        consumerEdge.pushSupply(newSupply, false, resourceType);
    }

    @Override
    public void handleIncomingDemand(FlowEdge consumerEdge, double newDemand) {
        handleIncomingDemand(consumerEdge, newDemand, consumerEdge.getConsumer().getResourceType());
    }

    @Override
    public void handleIncomingDemand(FlowEdge consumerEdge, double newPowerDemand, ResourceType resourceType) {
        updateCounters();
        this.powerDemandsPerResource.put(resourceType, new ArrayList<>(List.of(newPowerDemand)));

        pushOutgoingDemand(this.powerSupplyEdge, newPowerDemand);
    }

    @Override
    public void handleIncomingSupply(FlowEdge supplierEdge, double newSupply) {
        updateCounters();
        for (ResourceType resourceType : this.resourceEdges.keySet()) {
            for (FlowEdge edge : this.resourceEdges.get(resourceType)) {
                double outgoingSupply = Math.min(this.powerDemandsPerResource.get(resourceType).getFirst(), newSupply);
                pushOutgoingSupply(edge, outgoingSupply, resourceType);
            }
        }
    }

    @Override
    public void addConsumerEdge(FlowEdge consumerEdge) {
        ResourceType consumerResourceType = consumerEdge.getConsumer().getResourceType();
        this.resourceEdges.put(consumerResourceType, new ArrayList<>(List.of(consumerEdge)));
        this.powerDemandsPerResource.put(consumerResourceType, new ArrayList<>(List.of(0.0)));
        this.powerSuppliedPerResource.put(consumerResourceType, new ArrayList<>(List.of(0.0)));
    }

    @Override
    public void addSupplierEdge(FlowEdge supplierEdge) {
        this.powerSupplyEdge = supplierEdge;
    }

    @Override
    public void removeConsumerEdge(FlowEdge consumerEdge) {
        ResourceType resourceType = consumerEdge.getConsumer().getResourceType();
        if (this.resourceEdges.containsKey(resourceType)) {
            this.resourceEdges.remove(resourceType);
            this.powerDemandsPerResource.remove(resourceType);
            this.powerSuppliedPerResource.remove(resourceType);
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
            List<FlowEdge> edges = this.resourceEdges.get(resourceType);
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
    public ResourceType getResourceType() {
        return ResourceType.AUXILIARY;
    }
}
