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

import java.util.ArrayList;
import org.opendc.simulator.engine.engine.FlowEngine;
import org.opendc.simulator.engine.graph.FlowDistributor;
import org.opendc.simulator.engine.graph.FlowEdge;

/**
 * A {@link FlowDistributor} that implements the First Fit policy for distributing flow.
 *
 * This distributor allocates resources to consumers based on the first available supply that meets their demand.
 * It does not attempt to balance loads or optimize resource usage beyond the first fit principle.
 * It tries to place demands on already existing supplies without creating new ones.
 * It assumes that resources can be partitioned, if one supplier cannot satisfy the demand, it will try to combine multiple suppliers.
 */
public class FirstFitPolicyFlowDistributor extends FlowDistributor {

    public FirstFitPolicyFlowDistributor(FlowEngine engine) {
        super(engine);
    }

    /**
     * Updates the outgoing demand for suppliers in sequential order by their index.
     * With each supplier being allocated up to its full capacity before moving to the next supplier.
     */
    @Override
    protected void updateOutgoingDemand() {
        double remainingDemand = this.totalIncomingDemand;

        // Sort supplier edges by their index to ensure consistent first-fit ordering
        var sortedSuppliers = this.supplierEdges.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e1.getKey(), e2.getKey()))
                .toList();

        // Apply First Fit strategy: fill suppliers in order until demand is satisfied
        for (var supplierEntry : sortedSuppliers) {
            var supplierEdge = supplierEntry.getValue();
            double supplierCapacity = supplierEdge.getCapacity();

            if (remainingDemand <= 0) {
                // No more demand to allocate
                this.pushOutgoingDemand(supplierEdge, 0.0);
            } else if (remainingDemand <= supplierCapacity) {
                // This supplier can handle all remaining demand
                this.pushOutgoingDemand(supplierEdge, remainingDemand);
                remainingDemand = 0;
            } else {
                // This supplier gets filled to capacity, demand continues to next supplier
                this.pushOutgoingDemand(supplierEdge, supplierCapacity);
                remainingDemand -= supplierCapacity;
            }
        }

        this.outgoingDemandUpdateNeeded = false;
    }

    /**
     * Consumers receive their full demanded amount if it can be satisfied by the available supply,
     * or zero if it cannot.
     */
    @Override
    protected void updateOutgoingSupplies() {
        ArrayList<Double> currentPossibleSupplies = new ArrayList<>();
        for (var currentIncomingSupply : currentIncomingSupplies.entrySet()) {
            currentPossibleSupplies.add(currentIncomingSupply.getValue());
        }

        double[] shares = distributeSupply(incomingDemands, currentPossibleSupplies, totalIncomingSupply);

        for (FlowEdge consumerEdge : this.consumerEdges) {
            this.pushOutgoingSupply(consumerEdge, shares[consumerEdge.getConsumerIndex()]);
        }
    }

    /**
     * Distributes supply among consumers using the First Fit allocation principle.
     * Each consumer demand is allocated by trying suppliers in order, potentially
     * combining multiple suppliers to satisfy a single demand.
     *
     * @param demands List of demand values from consumers
     * @param currentSupply List of available supply values from suppliers
     * @param totalSupply Total amount of supply available (unused in this implementation)
     * @return Array of allocation amounts for each consumer
     *
     * @see #updateOutgoingSupplies()
     */
    @Override
    public double[] distributeSupply(ArrayList<Double> demands, ArrayList<Double> currentSupply, double totalSupply) {
        int numConsumers = demands.size();
        double[] allocation = new double[numConsumers];

        // Create a copy of current supply to track remaining capacity as we allocate
        ArrayList<Double> remainingSupply = new ArrayList<>(currentSupply);

        // For each demand, try to satisfy it using suppliers in order
        for (int i = 0; i < numConsumers; i++) {
            double remainingDemand = demands.get(i);
            double totalAllocated = 0.0;

            if (remainingDemand > 0) {
                // Try each supplier in order until demand is satisfied
                for (int j = 0; j < remainingSupply.size() && remainingDemand > 0; j++) {
                    double availableSupply = remainingSupply.get(j);

                    if (availableSupply > 0) {
                        // Allocate as much as possible from this supplier
                        double allocatedFromThisSupplier = Math.min(availableSupply, remainingDemand);

                        totalAllocated += allocatedFromThisSupplier;
                        remainingDemand -= allocatedFromThisSupplier;

                        // Reduce the remaining supply capacity
                        remainingSupply.set(j, availableSupply - allocatedFromThisSupplier);
                    }
                }
            }

            allocation[i] = totalAllocated;
        }

        return allocation;
    }
}
