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

package org.opendc.simulator.engine;

/**
 * An edge that connects two FlowStages.
 * A connection between FlowStages always consist of a FlowStage that demands
 * something, and a FlowStage that Delivers something
 * For instance, this could be the connection between a workload, and its machine
 */
public class FlowEdge {
    private FlowConsumer consumer;
    private FlowSupplier supplier;

    private double demand = 0.0;
    private double supply = 0.0;

    private double capacity;

    public FlowEdge(FlowConsumer consumer, FlowSupplier supplier) {
        if (!(consumer instanceof FlowNode)) {
            throw new IllegalArgumentException("Flow consumer is not a FlowNode");
        }
        if (!(supplier instanceof FlowNode)) {
            throw new IllegalArgumentException("Flow consumer is not a FlowNode");
        }

        this.consumer = consumer;
        this.supplier = supplier;

        this.capacity = supplier.getCapacity();

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
     * Push new demand from the Consumer to the Supplier
     */
    public void pushDemand(double newDemand) {

        this.demand = newDemand;
        this.supplier.handleDemand(this, newDemand);
        ((FlowNode) this.supplier).invalidate();
    }

    /**
     * Push new supply from the Supplier to the Consumer
     */
    public void pushSupply(double newSupply) {

        this.supply = newSupply;
        this.consumer.handleSupply(this, newSupply);
        ((FlowNode) this.consumer).invalidate();
    }
}
