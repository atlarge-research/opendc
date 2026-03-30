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

    protected final boolean[] updatedDemands;
    protected int numUpdatedDemands = 0;

    protected double previousTotalDemand = 0.0;
    protected double totalIncomingDemand; // The total demand of all the consumers

    protected final ArrayList<Integer> availableSupplierIndices;
    protected final ArrayList<Integer> usedSupplierIndices;

    protected int numSuppliers = 0;
    protected final int maxSuppliers;
    protected final FlowEdge[] supplierEdges;
    protected final double[] incomingSupplies;
    protected Double totalIncomingSupply = 0.0; // The total supply provided by the suppliers

    protected boolean outgoingDemandUpdateNeeded = false;
    protected boolean outgoingSupplyUpdateNeeded = false;

    protected ResourceType supplierResourceType;
    protected ResourceType consumerResourceType;

    protected double capacity; // What is the max capacity. Can probably be removed

    protected static HashMap<Integer, Integer> updateMap = new HashMap<Integer, Integer>();

    protected boolean overloaded = false;

    public FlowDistributor(FlowEngine engine, int maxConsumers, int maxSuppliers) {
        super(engine);

        this.maxConsumers = maxConsumers;
        this.maxSuppliers = 4;

        this.availableConsumerIndices = new ArrayList<>(this.maxConsumers);
        this.usedConsumerIndices = new ArrayList<>(this.maxConsumers);

        for (int i = 0; i < this.maxConsumers; i++) {
            this.availableConsumerIndices.add(i);
        }

        this.availableSupplierIndices = new ArrayList<>(this.maxSuppliers);
        this.usedSupplierIndices = new ArrayList<>(this.maxSuppliers);

        for (int i = 0; i < this.maxSuppliers; i++) {
            this.availableSupplierIndices.add(i);
        }

        this.consumerEdges = new FlowEdge[this.maxConsumers];
        this.supplierEdges = new FlowEdge[this.maxSuppliers];

        this.incomingSupplies = new double[this.maxSuppliers];
        this.incomingDemands = new double[this.maxConsumers];
        this.outgoingSupplies = new double[this.maxConsumers];

        this.updatedDemands = new boolean[this.maxConsumers];
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

        if (this.numUpdatedDemands > 0 || this.overloaded) {
            this.updateOutgoingSupplies();
        }

        return Long.MAX_VALUE;
    }

    protected abstract void updateOutgoingDemand();

    protected abstract void updateOutgoingSupplies();

    public abstract double[] distributeSupply(double[] demands, double[] currentSupply, double totalSupply);

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
    }

    @Override
    public void addSupplierEdge(FlowEdge supplierEdge) {
        int supplierIndex = this.availableSupplierIndices.removeFirst();
        this.usedSupplierIndices.add(supplierIndex);

        supplierEdge.setSupplierIndex(supplierIndex);
        this.supplierEdges[supplierIndex] = supplierEdge;

        this.numSuppliers++;

        this.capacity += supplierEdge.getCapacity();
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

        this.updatedDemands[consumerIndex] = false;

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
        int supplierIndex = supplierEdge.getSupplierIndex();

        if (supplierIndex == -1) {
            return;
        }

        this.capacity -= supplierEdge.getCapacity();

        this.supplierEdges[supplierIndex] = null;
        this.incomingSupplies[supplierIndex] = 0.0;

        this.usedSupplierIndices.remove(Integer.valueOf(supplierIndex));
        this.availableSupplierIndices.add(supplierIndex);

        this.numSuppliers--;

        this.outgoingDemandUpdateNeeded = true;
        this.invalidate();
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

        this.updatedDemands[consumerIndex] = true;
        this.numUpdatedDemands++;

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
        int supplierIndex = supplierEdge.getSupplierIndex();

        if (supplierIndex == -1) {
            LOGGER.warn("Demand {} pushed by an unknown supplier", newSupply);
            return;
        }

        double prevSupply = incomingSupplies[supplierIndex];

        incomingSupplies[supplierIndex] = newSupply;
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
                Arrays.asList(this.supplierEdges));
    }

    @Override
    public ResourceType getSupplierResourceType() {
        return this.supplierResourceType;
    }

    @Override
    public ResourceType getConsumerResourceType() {
        return this.consumerResourceType;
    }

    public Boolean hasSupplierEdges() {
        for (FlowEdge edge : this.supplierEdges) {
            if (edge != null) {
                return true;
            }
        }
        return false;
    }
}
