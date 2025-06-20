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

import org.opendc.common.ResourceType;

/**
 * An edge that connects two FlowStages.
 * A connection between FlowStages always consist of a FlowStage that demands
 * something, and a FlowStage that Delivers something
 * For instance, this could be the connection between a workload, and its machine
 */
public class FlowEdge {
    private FlowConsumer consumer;
    private FlowSupplier supplier;

    private int consumerIndex = -1;
    private int supplierIndex = -1;

    private double demand = 0.0;
    private double supply = 0.0;

    private final double capacity;

    private final ResourceType resourceType;

    public enum NodeType {
        CONSUMING,
        SUPPLYING
    }

    public FlowEdge(FlowConsumer consumer, FlowSupplier supplier) {
        this(consumer, supplier, ResourceType.AUXILIARY);
    }

    public FlowEdge(FlowConsumer consumer, FlowSupplier supplier, ResourceType resourceType) {
        this(consumer, supplier, resourceType, -1, -1);
    }

    public FlowEdge(FlowConsumer consumer, FlowSupplier supplier, ResourceType resourceType, int consumerIndex, int supplierIndex) {
        if (!(consumer instanceof FlowNode)) {
            throw new IllegalArgumentException("Flow consumer is not a FlowNode");
        }
        if (!(supplier instanceof FlowNode)) {
            throw new IllegalArgumentException("Flow consumer is not a FlowNode");
        }

        this.consumer = consumer;
        this.supplier = supplier;
        this.resourceType = resourceType;

        this.capacity = supplier.getCapacity(resourceType);

        // to avoid race condition of setting indices and requiring them in the PSU
        this.supplierIndex = supplierIndex;
        this.consumerIndex = consumerIndex;

        this.consumer.addSupplierEdge(this);
        this.supplier.addConsumerEdge(this);
    }

    public void close() {
        if (this.consumer != null) {
            this.consumer.removeSupplierEdge(this);
            this.consumer = null;
        }

        if (this.supplier != null) {
            this.supplier.removeConsumerEdge(this);
            this.supplier = null;
        }
    }

    /**
     * Close the edge of the specified node type.
     *
     * @param nodeType The type of connected node that is being closed.
     */
    public void close(NodeType nodeType) {
        if (nodeType == NodeType.CONSUMING) {
            this.consumer = null;
            this.supplier.removeConsumerEdge(this);
            this.supplier = null;
        }
        if (nodeType == NodeType.SUPPLYING) {
            this.supplier = null;
            this.consumer.removeSupplierEdge(this);
            this.consumer = null;
        }
    }

    public FlowConsumer getConsumer() {
        return consumer;
    }

    public FlowSupplier getSupplier() {
        return supplier;
    }

    public double getCapacity() {
        return capacity;
    }

    public double getDemand() {
        return this.demand;
    }

    public double getSupply() {
        return this.supply;
    }

    /**
     * Get the resource type of this edge.
     *
     * @return The resource type of this edge.
     */
    public ResourceType getResourceType() {
        return this.resourceType;
    }

    /**
     * Get the resource type of the supplier of this edge.
     *
     * @return The resource type of the supplier.
     */
    public ResourceType getSupplierResourceType() {
        return this.supplier.getSupplierResourceType();
    }

    /**
     * Get the resource type of the consumer of this edge.
     *
     * @return The resource type of the consumer.
     */
    public ResourceType getConsumerResourceType() {
        return this.consumer.getConsumerResourceType();
    }

    public int getConsumerIndex() {
        return consumerIndex;
    }

    public void setConsumerIndex(int consumerIndex) {
        this.consumerIndex = consumerIndex;
    }

    public int getSupplierIndex() {
        return supplierIndex;
    }

    public void setSupplierIndex(int supplierIndex) {
        this.supplierIndex = supplierIndex;
    }

    public void pushDemand(double newDemand, boolean forceThrough, ResourceType resourceType) {
        // or store last resource type in the edge
        if ((newDemand == this.demand) && !forceThrough) {
            return;
        }

        this.demand = newDemand;
        this.supplier.handleIncomingDemand(this, newDemand, resourceType);
    }

    /**
     * Push new demand from the Consumer to the Supplier
     */
    public void pushDemand(double newDemand, boolean forceThrough) {
        if ((newDemand == this.demand) && !forceThrough) {
            return;
        }

        this.demand = newDemand;
        this.supplier.handleIncomingDemand(this, newDemand);
    }

    /**
     * Push new demand from the Consumer to the Supplier
     */
    public void pushDemand(double newDemand) {
        this.pushDemand(newDemand, false);
    }

    /**
     * Push new supply from the Supplier to the Consumer
     */
    public void pushSupply(double newSupply, boolean forceThrough, ResourceType resourceType) {
        if ((newSupply == this.supply) && !forceThrough) {
            return;
        }

        this.supply = newSupply;
        this.consumer.handleIncomingSupply(this, newSupply, resourceType);
    }

    /**
     * Push new supply from the Supplier to the Consumer
     */
    public void pushSupply(double newSupply) {
        this.pushSupply(newSupply, false, this.supplier.getSupplierResourceType());
    }
}
