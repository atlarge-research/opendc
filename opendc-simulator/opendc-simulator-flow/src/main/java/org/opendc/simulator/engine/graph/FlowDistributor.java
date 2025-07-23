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

package org.opendc.simulator.engine.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendc.common.ResourceType;
import org.opendc.simulator.engine.engine.FlowEngine;
import org.opendc.simulator.engine.graph.distributionPolicies.FlowDistributorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link FlowDistributor} is a node that distributes supply from multiple suppliers to multiple consumers.
 * It can be used to model host-level resource distribution, such as CPU, memory, and GPU distribution.
 * It is a subclass of {@link FlowNode} and implements both {@link FlowSupplier} and {@link FlowConsumer}.
 * It maintains a list of consumer edges and supplier edges, and it can handle incoming demands and supplies.
 * It also provides methods to update outgoing demands and supplies based on the incoming demands and supplies.
 * This class is abstract and should be extended by specific implementations that define the distribution strategy.
 * It uses a {@link FlowDistributorFactory.DistributionPolicy} to determine how to distribute the supply among the consumers.
 * The default distribution policy is MaxMinFairnessPolicy, which distributes the supply fairly among the consumers.
 */
public abstract class FlowDistributor extends FlowNode implements FlowSupplier, FlowConsumer {
    protected static final Logger LOGGER = LoggerFactory.getLogger(FlowDistributor.class);
    protected final ArrayList<FlowEdge> consumerEdges = new ArrayList<>();
    protected HashMap<Integer, FlowEdge> supplierEdges =
            new HashMap<>(); // The suppliers that provide supply to this distributor

    protected final ArrayList<Double> incomingDemands = new ArrayList<>(); // What is demanded by the consumers
    protected final ArrayList<Double> outgoingSupplies = new ArrayList<>(); // What is supplied to the consumers

    protected double totalIncomingDemand; // The total demand of all the consumers
    // AS index is based on the supplierIndex of the FlowEdge, ids of entries need to be stable
    protected HashMap<Integer, Double> currentIncomingSupplies =
            new HashMap<>(); // The current supply provided by the suppliers
    protected Double totalIncomingSupply = 0.0; // The total supply provided by the suppliers

    protected boolean outgoingDemandUpdateNeeded = false;
    protected boolean outgoingSupplyUpdateNeeded = false;
    protected Set<Integer> updatedDemands =
            new HashSet<>(); // Array of consumers that updated their demand in this cycle

    protected ResourceType supplierResourceType;
    protected ResourceType consumerResourceType;

    protected double capacity; // What is the max capacity. Can probably be removed

    public FlowDistributor(FlowEngine engine) {
        super(engine);
    }

    public double getTotalIncomingDemand() {
        return totalIncomingDemand;
    }

    public double getCurrentIncomingSupply() {
        return this.totalIncomingSupply;
    }

    public double getCapacity() {
        return capacity;
    }

    public long onUpdate(long now) {

        // Check if current supply is different from total demand
        if (this.outgoingDemandUpdateNeeded) {
            this.updateOutgoingDemand();

            return Long.MAX_VALUE;
        }

        if (!this.outgoingSupplies.isEmpty()) {
            this.updateOutgoingSupplies();
        }

        return Long.MAX_VALUE;
    }

    protected abstract void updateOutgoingDemand();

    protected abstract void updateOutgoingSupplies();

    public abstract double[] distributeSupply(
            ArrayList<Double> demands, ArrayList<Double> currentSupply, double totalSupply);

    /**
     * Add a new consumer.
     * Set its demand and supply to 0.0
     */
    @Override
    public void addConsumerEdge(FlowEdge consumerEdge) {
        consumerEdge.setConsumerIndex(this.consumerEdges.size());

        this.consumerEdges.add(consumerEdge);
        this.incomingDemands.add(0.0);
        this.outgoingSupplies.add(0.0);
        this.consumerResourceType = consumerEdge.getConsumerResourceType();
        this.outgoingDemandUpdateNeeded = true;
    }

    @Override
    public void addSupplierEdge(FlowEdge supplierEdge) {
        // supplierIndex not always set, so we use 0 as default to avoid index out of bounds
        int idx = supplierEdge.getSupplierIndex() == -1 ? 0 : supplierEdge.getSupplierIndex();

        this.supplierEdges.put(idx, supplierEdge);
        this.capacity += supplierEdge.getCapacity();
        this.currentIncomingSupplies.put(idx, 0.0);
        this.supplierResourceType = supplierEdge.getSupplierResourceType();
    }

    @Override
    public void removeConsumerEdge(FlowEdge consumerEdge) {
        int idx = consumerEdge.getConsumerIndex();

        if (idx == -1) {
            return;
        }

        this.totalIncomingDemand -= consumerEdge.getDemand();

        // Remove idx from consumers that updated their demands
        this.updatedDemands.remove(idx);

        this.consumerEdges.remove(idx);
        this.incomingDemands.remove(idx);
        this.outgoingSupplies.remove(idx);

        // update the consumer index for all consumerEdges higher than this.
        for (int i = idx; i < this.consumerEdges.size(); i++) {
            FlowEdge other = this.consumerEdges.get(i);

            other.setConsumerIndex(other.getConsumerIndex() - 1);
        }

        HashSet<Integer> newUpdatedDemands = new HashSet<>();

        for (int idx_other : this.updatedDemands) {
            if (idx_other > idx) {
                newUpdatedDemands.add(idx_other - 1);
            } else {
                newUpdatedDemands.add(idx_other);
            }
        }

        this.updatedDemands = newUpdatedDemands;

        this.outgoingDemandUpdateNeeded = true;
        this.invalidate();
    }

    @Override
    public void removeSupplierEdge(FlowEdge supplierEdge) {
        // supplierIndex not always set, so we use 0 as default to avoid index out of bounds
        int idx = supplierEdge.getSupplierIndex() == -1 ? 0 : supplierEdge.getSupplierIndex();
        // to keep index consistent, entries are neutralized instead of removed
        //        this.supplierEdges.put(idx, null);

        this.supplierEdges.remove(idx);
        this.capacity -= supplierEdge.getCapacity();
        this.currentIncomingSupplies.put(idx, 0.0);

        if (this.supplierEdges.isEmpty()) {
            this.updatedDemands.clear();
        }
    }

    @Override
    public void handleIncomingDemand(FlowEdge consumerEdge, double newDemand) {
        int idx = consumerEdge.getConsumerIndex();

        if (idx == -1) {
            LOGGER.warn("Demand {} pushed by an unknown consumer", newDemand);
            return;
        }

        // Update the total demand (This is cheaper than summing over all demands)
        double prevDemand = incomingDemands.get(idx);

        incomingDemands.set(idx, newDemand);
        // only update the total supply if the new supply is different from the previous one
        this.totalIncomingDemand += (newDemand - prevDemand);

        this.updatedDemands.add(idx);

        this.outgoingDemandUpdateNeeded = true;
        this.invalidate();
    }

    @Override
    public void handleIncomingDemand(FlowEdge consumerEdge, double newDemand, ResourceType resourceType) {
        if (resourceType != this.getSupplierResourceType()) {
            throw new IllegalArgumentException("Resource type " + resourceType
                    + " does not match distributor resource type " + this.getSupplierResourceType());
        }
        this.handleIncomingDemand(consumerEdge, newDemand);
    }

    @Override
    public void handleIncomingSupply(FlowEdge supplierEdge, double newSupply) {
        // supplierIndex not always set, so we use 0 as default to avoid index out of bounds
        int idx = supplierEdge.getSupplierIndex() == -1 ? 0 : supplierEdge.getSupplierIndex();
        double prevSupply = currentIncomingSupplies.get(idx);

        currentIncomingSupplies.put(idx, newSupply);
        // only update the total supply if the new supply is different from the previous one
        this.totalIncomingSupply += (newSupply - prevSupply);

        this.outgoingSupplyUpdateNeeded = true;
        this.invalidate();
    }

    @Override
    public void pushOutgoingDemand(FlowEdge supplierEdge, double newDemand) {
        supplierEdge.pushDemand(newDemand, false, this.getSupplierResourceType(), this.consumerEdges.size());
    }

    @Override
    public void pushOutgoingSupply(FlowEdge consumerEdge, double newSupply) {
        int idx = consumerEdge.getConsumerIndex();

        if (idx == -1) {
            System.out.println("Error (FlowDistributor): pushing supply to an unknown consumer");
        }

        if (outgoingSupplies.get(idx) == newSupply) {
            return;
        }

        outgoingSupplies.set(idx, newSupply);
        consumerEdge.pushSupply(newSupply, false, this.getSupplierResourceType());
    }

    @Override
    public Map<FlowEdge.NodeType, List<FlowEdge>> getConnectedEdges() {
        return Map.of(
                FlowEdge.NodeType.CONSUMING,
                this.consumerEdges,
                FlowEdge.NodeType.SUPPLYING,
                new ArrayList<>(this.supplierEdges.values()));
    }

    @Override
    public ResourceType getSupplierResourceType() {
        //        return this.supplierEdge.getSupplierResourceType();
        return this.supplierResourceType;
    }

    @Override
    public ResourceType getConsumerResourceType() {
        return this.consumerResourceType;
    }

    public Boolean hasSupplierEdges() {
        for (FlowEdge edge : this.supplierEdges.values()) {
            if (edge != null) {
                return true;
            }
        }
        return false;
    }
}
