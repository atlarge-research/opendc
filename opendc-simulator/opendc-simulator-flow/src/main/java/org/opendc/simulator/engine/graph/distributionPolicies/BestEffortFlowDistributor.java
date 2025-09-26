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
 * A Best Effort Flow Distributor that implements a timesliced round-robin approach.
 *
 * Key principles:
 * - Timesliced Round Robin: Resources are allocated to consumers in a round-robin manner
 * - Non-Guaranteed Shares: No fixed allocation per consumer, distribution based on current demand
 * - Optimized Utilization: Maximizes resource utilization during idle periods
 *
 * This scheduler is suitable for environments with fluctuating workloads where fairness
 * is less important than maximizing overall resource utilization.
 * <a href="https://docs.nvidia.com/vgpu/knowledge-base/latest/vgpu-features.html#vgpu-schedulers">original description</a>
 */
public class BestEffortFlowDistributor extends FlowDistributor {

    private int currentRoundRobinIndex = 0;
    private boolean overloaded = false;
    private final long roundRobinInterval;
    private long lastRoundRobinUpdate;

    public BestEffortFlowDistributor(FlowEngine flowEngine, long roundRobinInterval, int maxConsumers) {
        super(flowEngine, maxConsumers);
        this.roundRobinInterval = roundRobinInterval;
        this.lastRoundRobinUpdate = -roundRobinInterval;
    }

    /**
     * Updates the outgoing demand based on the total incoming demand.
     * Prioritizes already utilized suppliers when potential supply exceeds demand.
     */
    @Override
    protected void updateOutgoingDemand() {

        // If potential supply exceeds demand, prioritize already utilized suppliers
        if (this.capacity > this.totalIncomingDemand && this.totalIncomingDemand > 0) {
            // Best-effort: try to satisfy demand using already active suppliers first
            double remainingDemand = this.totalIncomingDemand;

            // Phase 1: Prioritize suppliers that are currently providing supply
            for (var entry : this.supplierEdges.entrySet()) {
                int supplierIdx = entry.getKey();
                FlowEdge supplierEdge = entry.getValue();
                double currentSupply = this.currentIncomingSupplies.get(supplierIdx);

                if (currentSupply > 0 && remainingDemand > 0) {
                    // Try to satisfy as much demand as possible from this already active supplier
                    double demandForThisSupplier = Math.min(remainingDemand, supplierEdge.getCapacity());
                    this.pushOutgoingDemand(supplierEdge, demandForThisSupplier);
                    remainingDemand -= demandForThisSupplier;
                }
            }

            // Phase 2: If demand still remains, use inactive suppliers
            if (remainingDemand > 0) {
                for (var entry : this.supplierEdges.entrySet()) {
                    int supplierIdx = entry.getKey();
                    FlowEdge supplierEdge = entry.getValue();
                    double currentSupply = this.currentIncomingSupplies.get(supplierIdx);

                    if (currentSupply == 0 && remainingDemand > 0) {
                        double demandForThisSupplier = Math.min(remainingDemand, supplierEdge.getCapacity());
                        this.pushOutgoingDemand(supplierEdge, demandForThisSupplier);
                        remainingDemand -= demandForThisSupplier;
                    }
                }
            }
        } else {
            // System is overloaded or no demand: distribute demand equally across all suppliers
            double demandPerSupplier = this.totalIncomingDemand / this.supplierEdges.size();

            for (FlowEdge supplierEdge : this.supplierEdges.values()) {
                this.pushOutgoingDemand(supplierEdge, demandPerSupplier);
            }
        }

        this.outgoingDemandUpdateNeeded = false;
        this.invalidate();
    }

    /**
     * Updates the outgoing supplies using a best-effort approach.
     * When overloaded, uses round-robin distribution. Otherwise, satisfies demands optimally.
     */
    @Override
    protected void updateOutgoingSupplies() {
        // Check if system is overloaded (demand exceeds supply)
        if (this.totalIncomingDemand > this.totalIncomingSupply) {
            this.overloaded = true;

            // Use the distribution algorithm for supply allocation
            double[] supplies = this.distributeSupply(
                    this.incomingDemands,
                    new ArrayList<>(this.currentIncomingSupplies.values()),
                    this.totalIncomingSupply);

            for (int consumerIndex : this.usedConsumerIndices) {
                this.pushOutgoingSupply(
                        this.consumerEdges[consumerIndex], supplies[consumerIndex], this.getConsumerResourceType());
            }
        } else {
            // System is not overloaded - satisfy all demands and utilize remaining capacity

            if (this.overloaded) {
                for (int consumerIndex : this.usedConsumerIndices) {
                    // TODO: I think we can remove this check
                    if (this.outgoingSupplies[consumerIndex] == this.incomingDemands[consumerIndex]) {
                        this.pushOutgoingSupply(
                                this.consumerEdges[consumerIndex],
                                this.incomingDemands[consumerIndex],
                                this.getConsumerResourceType());
                    }
                }
                this.overloaded = false;
            }

            // Update the supplies of the consumers that changed their demand in the current cycle
            else {
                for (int consumerIndex : this.updatedDemands) {
                    this.pushOutgoingSupply(
                            this.consumerEdges[consumerIndex],
                            this.incomingDemands[consumerIndex],
                            this.getConsumerResourceType());
                }
            }
        }

        this.outgoingSupplyUpdateNeeded = false;
        this.updatedDemands.clear();
    }

    /**
     * Distributes available supply using a best-effort, round-robin approach.
     * Algorithm:
     * 1. First pass: Satisfy demands up to available capacity in round-robin order
     * 2. Second pass: Distribute remaining capacity to consumers with unsatisfied demand
     * 3. Optimize utilization by giving extra capacity to active consumers
     */
    @Override
    public double[] distributeSupply(double[] demands, ArrayList<Double> currentSupply, double totalSupply) {
        int numConsumers = this.consumerEdges.length;
        double[] allocation = new double[numConsumers];

        if (numConsumers == 0 || totalSupply <= 0) {
            return allocation;
        }

        double remainingSupply = totalSupply;

        // Phase 1: Round-robin distribution based on demand
        // Start from the current round-robin index to ensure fairness over time
        for (int round = 0; round < numConsumers && remainingSupply > 0; round++) {
            int idx = (currentRoundRobinIndex + round) % numConsumers;
            double demand = demands[idx];

            if (demand > allocation[idx]) {
                // Calculate how much we can allocate in this round
                double unmetDemand = demand - allocation[idx];
                double toAllocate = Math.min(unmetDemand, remainingSupply);

                allocation[idx] += toAllocate;
                remainingSupply -= toAllocate;
            }
        }

        // Phase 2: Distribute any remaining supply to maximize utilization
        // Give preference to consumers with the highest relative demand
        if (remainingSupply > 0) {
            // Create a list of consumers with unsatisfied demand, sorted by relative need
            ArrayList<Integer> unsatisfiedConsumers = new ArrayList<>();
            for (int i = 0; i < numConsumers; i++) {
                if (demands[i] > allocation[i]) {
                    unsatisfiedConsumers.add(i);
                }
            }

            // If no unsatisfied demand, distribute remaining capacity equally among active consumers
            if (unsatisfiedConsumers.isEmpty()) {
                // Find consumers with any demand (active consumers)
                ArrayList<Integer> activeConsumers = new ArrayList<>();
                for (int i = 0; i < numConsumers; i++) {
                    if (demands[i] > 0) {
                        activeConsumers.add(i);
                    }
                }

                if (!activeConsumers.isEmpty()) {
                    double extraPerConsumer = remainingSupply / activeConsumers.size();
                    for (int idx : activeConsumers) {
                        allocation[idx] += extraPerConsumer;
                    }
                }
            } else {
                // Distribute remaining supply proportionally to unsatisfied demand
                double totalUnsatisfiedDemand = 0;
                for (int idx : unsatisfiedConsumers) {
                    totalUnsatisfiedDemand += demands[idx] - allocation[idx];
                }

                for (int idx : unsatisfiedConsumers) {
                    double unsatisfiedDemand = demands[idx] - allocation[idx];
                    double proportion = unsatisfiedDemand / totalUnsatisfiedDemand;
                    allocation[idx] += remainingSupply * proportion;
                }
            }
        }

        // Update round-robin index for next allocation cycle
        if (numConsumers > 0) {
            currentRoundRobinIndex = (currentRoundRobinIndex + 1) % numConsumers;
        }

        return allocation;
    }

    /**
     * Enhanced onUpdate method that implements time-sliced round-robin scheduling.
     * This method ensures the round-robin index advances at regular intervals,
     * creating true time-sliced behavior for best-effort scheduling.
     */
    @Override
    public long onUpdate(long now) {
        long nextUpdate = Long.MAX_VALUE;

        boolean updateNeeded = false;

        // Check if it's time for a round-robin advancement
        if (now >= lastRoundRobinUpdate + this.roundRobinInterval) {
            updateNeeded = true;
            lastRoundRobinUpdate = now;

            // Schedule next round-robin update
            nextUpdate = now + this.roundRobinInterval;
        } else {
            // Schedule the next potential round-robin update
            nextUpdate = lastRoundRobinUpdate + this.roundRobinInterval;
        }

        // Update demands if needed
        if (this.outgoingDemandUpdateNeeded || updateNeeded) {
            this.updateOutgoingDemand();
        }

        // Update supplies if needed
        if (this.outgoingSupplyUpdateNeeded || updateNeeded) {
            this.updateOutgoingSupplies();
        }

        if (this.numConsumers == 0 || !this.hasSupplierEdges()) {
            nextUpdate = Long.MAX_VALUE;
        }

        return nextUpdate;
    }
}
