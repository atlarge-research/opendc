/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.simulator.engine.graph.distributionPolicies;

import java.util.Arrays;
import org.opendc.simulator.engine.engine.FlowEngine;
import org.opendc.simulator.engine.graph.FlowDistributor;

/**
 * A {@link FlowDistributor} that implements the Equal Share distribution policy.
 * <p>
 * This distributor allocates resources equally among all suppliers and consumers, ensuring that each supplier and
 * consumer receives an equal share of the total capacity.
 * <a href="https://docs.nvidia.com/vgpu/knowledge-base/latest/vgpu-features.html#vgpu-schedulers">original description</a>
 */
public class EqualShareFlowDistributor extends FlowDistributor {

    public EqualShareFlowDistributor(FlowEngine engine, int maxConsumers, int maxSuppliers) {
        super(engine, maxConsumers, maxSuppliers);
    }

    /**
     * Updates the outgoing demand for each supplier edge based on the total capacity.
     * The demand is equally distributed among all suppliers.
     * This method is called when the outgoing demand needs to be updated.
     */
    @Override
    protected void updateOutgoingDemand() {
        double equalShare = this.capacity / this.numSuppliers;

        for (int supplierIndex : this.usedSupplierIndices) {
            this.pushOutgoingDemand(this.supplierEdges[supplierIndex], equalShare);
        }

        this.outgoingDemandUpdateNeeded = false;
    }

    /**
     * Updates the outgoing supply for each consumer edge based on the total supply.
     * The supply is equally distributed among all consumers.
     * This method is called when the outgoing supply needs to be updated.
     */
    @Override
    protected void updateOutgoingSupplies() {
        double[] equalShare = distributeSupply(incomingDemands, this.incomingSupplies, this.capacity);

        for (int consumerIndex : this.usedConsumerIndices) {
            this.pushOutgoingSupply(
                    this.consumerEdges[consumerIndex], equalShare[consumerIndex], this.getConsumerResourceType());
        }

        Arrays.fill(this.updatedDemands, false);
        this.numUpdatedDemands = 0;
    }

    @Override
    public double[] distributeSupply(double[] demands, double[] currentSupply, double totalSupply) {
        double[] allocation = new double[this.numConsumers];
        double equalShare = totalSupply / this.numConsumers;

        // Equal share regardless of individual demands
        Arrays.fill(allocation, equalShare);

        return allocation;
    }
}
