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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    protected int numConsumers = 0;
    protected final int maxConsumers;

    protected final ArrayList<Integer> availableConsumerIndices;
    protected final ArrayList<Integer> usedConsumerIndices;

    protected final FlowEdge[] consumerEdges;
    protected final double[] incomingDemands; // What is demanded by the consumers
    protected final double[] outgoingSupplies; // What is supplied to the consumers
    protected ArrayList<Integer> updatedDemands = new ArrayList<>();

    protected double previousTotalDemand = 0.0;
    protected double totalIncomingDemand; // The total demand of all the consumers

    protected HashMap<Integer, FlowEdge> supplierEdges = new HashMap<>();
    protected HashMap<Integer, Double> currentIncomingSupplies =
            new HashMap<>(); // The current supply provided by the suppliers
    protected Double totalIncomingSupply = 0.0; // The total supply provided by the suppliers

    protected boolean outgoingDemandUpdateNeeded = false;
    protected boolean outgoingSupplyUpdateNeeded = false;

    protected ResourceType supplierResourceType;
    protected ResourceType consumerResourceType;

    protected double capacity; // What is the max capacity. Can probably be removed

    //    protected final ArrayList<FlowEdge> consumerEdges = new ArrayList<>();
    //    protected final ArrayList<Double> incomingDemands = new ArrayList<>(); // What is demanded by the consumers
    //    protected final ArrayList<Double> outgoingSupplies = new ArrayList<>(); // What is supplied to the consumers
    //
    //    protected double totalIncomingDemand; // The total demand of all the consumers
    //    protected HashMap<Integer, Double> currentIncomingSupplies = new HashMap<>(); // The current supply provided
    // by the suppliers
    //    protected Double totalIncomingSupply = 0.0; // The total supply provided by the suppliers
    //
    //    protected boolean outgoingDemandUpdateNeeded = false;
    //    protected boolean outgoingSupplyUpdateNeeded = false;
    //    protected Set<Integer> updatedDemands =
    //            new HashSet<>(); // Array of consumers that updated their demand in this cycle
    //
    //    protected ResourceType supplierResourceType;
    //    protected ResourceType consumerResourceType;
    //
    //    protected double capacity; // What is the max capacity. Can probably be removed

    public FlowDistributor(FlowEngine engine, int maxConsumers) {
        super(engine);

        this.maxConsumers = maxConsumers;

        this.availableConsumerIndices = new ArrayList<>(this.maxConsumers);
        this.usedConsumerIndices = new ArrayList<>(this.maxConsumers);

        for (int i = 0; i < this.maxConsumers; i++) {
            this.availableConsumerIndices.add(i);
        }

        this.consumerEdges = new FlowEdge[this.maxConsumers];
        //        this.supplierEdges = new FlowEdge[this.maxSuppliers];
        //        this.incomingSupplies = new double[this.maxSuppliers];

        this.incomingDemands = new double[this.maxConsumers];
        this.outgoingSupplies = new double[this.maxConsumers];
        //        this.updatedDemands = new boolean[this.maxConsumers];
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

        // TODO: look into whether this is always needed
        if (this.numConsumers > 0) {
            this.updateOutgoingSupplies();
        }

        return Long.MAX_VALUE;
    }

    protected abstract void updateOutgoingDemand();

    protected abstract void updateOutgoingSupplies();

    public abstract double[] distributeSupply(double[] demands, ArrayList<Double> currentSupply, double totalSupply);

    /**
     * Add a new consumer.
     * Set its demand and supply to 0.0
     */
    @Override
    public void addConsumerEdge(FlowEdge consumerEdge) {
        int consumerIndex = this.availableConsumerIndices.removeFirst();
        this.usedConsumerIndices.add(consumerIndex);

        consumerEdge.setConsumerIndex(consumerIndex);

        this.numConsumers++;
        this.consumerEdges[consumerIndex] = consumerEdge;
        this.outgoingDemandUpdateNeeded = true;
    }

    @Override
    public void addSupplierEdge(FlowEdge supplierEdge) {
        // supplierIndex not always set, so we use 0 as default to avoid index out of bounds
        int supplierIndex = supplierEdge.getSupplierIndex() == -1 ? 0 : supplierEdge.getSupplierIndex();

        this.supplierEdges.put(supplierIndex, supplierEdge);
        this.capacity += supplierEdge.getCapacity();
        this.currentIncomingSupplies.put(supplierIndex, 0.0);
        this.supplierResourceType = supplierEdge.getSupplierResourceType();
    }

    @Override
    public void removeConsumerEdge(FlowEdge consumerEdge) {
        int consumerIndex = consumerEdge.getConsumerIndex();

        if (consumerIndex == -1) {
            return;
        }

        this.totalIncomingDemand -= consumerEdge.getDemand();
        if (this.totalIncomingDemand < 0) {
            this.totalIncomingDemand = 0.0;
        }

        // Remove idx from consumers that updated their demands
        //        this.updatedDemands[consumerIndex] = false;
        if (this.updatedDemands.contains(consumerIndex)) {
            this.updatedDemands.remove(Integer.valueOf(consumerIndex));
        }

        this.consumerEdges[consumerIndex] = null;
        this.incomingDemands[consumerIndex] = 0.0;
        this.outgoingSupplies[consumerIndex] = 0.0;

        this.usedConsumerIndices.remove(Integer.valueOf(consumerIndex));
        this.availableConsumerIndices.add(consumerIndex);

        this.numConsumers--;

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
            //            Arrays.fill(this.updatedDemands, false);
        }
    }

    @Override
    public void handleIncomingDemand(FlowEdge consumerEdge, double newDemand) {
        int consumerIndex = consumerEdge.getConsumerIndex();

        if (consumerIndex == -1) {
            LOGGER.warn("Demand {} pushed by an unknown consumer", newDemand);
            return;
        }

        // Update the total demand (This is cheaper than summing over all demands)
        double prevDemand = incomingDemands[consumerIndex];

        incomingDemands[consumerIndex] = newDemand;
        // only update the total supply if the new supply is different from the previous one
        this.totalIncomingDemand += (newDemand - prevDemand);
        if (totalIncomingDemand < 0) {
            this.totalIncomingDemand = 0.0;
        }

        // TODO: can be optimized by using a boolean array
        this.updatedDemands.add(consumerIndex);

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
        supplierEdge.pushDemand(newDemand, false, this.getSupplierResourceType(), this.numConsumers);
    }

    @Override
    public void pushOutgoingSupply(FlowEdge consumerEdge, double newSupply) {
        int consumerIndex = consumerEdge.getConsumerIndex();

        if (consumerIndex == -1) {
            System.out.println("Error (FlowDistributor): pushing supply to an unknown consumer");
        }

        if (outgoingSupplies[consumerIndex] == newSupply) {
            return;
        }

        outgoingSupplies[consumerIndex] = newSupply;
        consumerEdge.pushSupply(newSupply, false, this.getSupplierResourceType());
    }

    @Override
    public Map<FlowEdge.NodeType, List<FlowEdge>> getConnectedEdges() {
        return Map.of(
                FlowEdge.NodeType.CONSUMING,
                Arrays.asList(this.consumerEdges),
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
