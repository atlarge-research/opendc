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

package org.opendc.simulator.engine.graph.distributionPolicies;

import java.util.ArrayList;
import java.util.HashSet;
import org.opendc.simulator.engine.engine.FlowEngine;
import org.opendc.simulator.engine.graph.FlowDistributor;
import org.opendc.simulator.engine.graph.FlowEdge;

/**
 * A {@link FlowDistributor} that implements Fixed Share GPU scheduling.
 *
 * This distributor allocates a dedicated, consistent portion of GPU resources to each VM (consumer),
 * ensuring predictable availability and stable performance. Each active consumer receives a fixed
 * share of the total GPU capacity, regardless of their individual demand or the demand of other consumers.
 * This policy is heavily inspired by the fixed share GPU scheduling policy used in NVIDIA's MIG (Multi-Instance GPU) technology.
 * Key characteristics:
 * - Each consumer gets a fixed percentage of total GPU capacity when active
 * - Unused shares (from inactive consumers) remain unallocated, not redistributed
 * - Performance remains consistent and predictable for each consumer
 * - Share allocation is based on maximum supported consumers, not currently active ones
 */
public class FixedShareFlowDistributor extends FlowDistributor {

    private double fixedShare;
    private final double shareRatio;
    private int[] notSuppliedConsumers;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a Fixed Share Flow Distributor.
     *
     * @param engine The flow engine
     * @param shareRatio The fixed share ratio for each consumer (between 0.0 and 1.0)
     */
    public FixedShareFlowDistributor(FlowEngine engine, double shareRatio) {
        super(engine);

        if (shareRatio <= 0 || shareRatio > 1) {
            throw new IllegalArgumentException("Share ratio must be between 0.0 and 1.0");
        }
        this.shareRatio = shareRatio;

        // Each consumer gets an equal fixed share of the total capacity
        this.fixedShare = this.shareRatio * this.capacity / this.supplierEdges.size();

        // Initialize tracking for round-robin prioritization
        this.notSuppliedConsumers = new int[0];
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Distribution Logic
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Updates the outgoing demand to suppliers.
     * In Fixed Share mode, we request the total amount needed to satisfy all active consumers' fixed shares.
     */
    @Override
    protected void updateOutgoingDemand() {
        // Calculate total demand based on active consumers
        this.fixedShare = this.shareRatio * this.capacity / this.supplierEdges.size();

        // Distribute demand equally across all suppliers
        for (FlowEdge supplierEdge : supplierEdges.values()) {
            this.pushOutgoingDemand(supplierEdge, this.fixedShare);
        }

        this.outgoingDemandUpdateNeeded = false;
        this.invalidate();
    }

    /**
     * Updates the outgoing supplies to consumers.
     * Each active consumer receives their fixed share, regardless of their actual demand.
     */
    @Override
    protected void updateOutgoingSupplies() {
        // Calculate the fixed allocation per consumer

        // Distribute to each consumer
        int consumerIndex = 0;
        if (this.consumerEdges.size() == this.supplierEdges.size()) {
            for (FlowEdge consumerEdge : this.consumerEdges) {
                this.pushOutgoingSupply(consumerEdge, this.fixedShare);
            }
        } else {
            double[] supplies = distributeSupply(this.incomingDemands, this.outgoingSupplies, this.totalIncomingSupply);
            for (FlowEdge consumerEdge : this.consumerEdges) {
                if (supplies[consumerIndex] <= 0.0) {
                    continue;
                }
                this.pushOutgoingSupply(consumerEdge, this.fixedShare);
            }
        }
    }

    public double[] distributeSupply(ArrayList<Double> demands, ArrayList<Double> currentSupply, double totalSupply) {
        double[] supplies = new double[this.consumerEdges.size()];

        if (this.consumerEdges.size() < this.supplierEdges.size()) {
            for (FlowEdge consumerEdge : this.consumerEdges) {
                supplies[consumerEdge.getConsumerIndex()] = this.fixedShare;
            }
        } else {
            // Round-robin approach: prioritize consumers that didn't get resources last time
            ArrayList<Integer> currentNotSuppliedList = new ArrayList<>();

            // Calculate how many consumers we can supply with available resources
            int maxConsumersToSupply = (int) Math.floor(totalSupply / this.fixedShare);
            int consumersSupplied = 0;

            // First pass: try to supply consumers that were not supplied in the previous round
            for (int index : this.notSuppliedConsumers) {
                if (index < this.consumerEdges.size() && consumersSupplied < maxConsumersToSupply) {
                    supplies[index] = this.fixedShare;
                    consumersSupplied++;
                }
            }

            // Second pass: supply remaining consumers if we still have capacity
            for (int i = 0; i < this.consumerEdges.size() && consumersSupplied < maxConsumersToSupply; i++) {
                if (supplies[i] == 0.0) { // This consumer hasn't been supplied yet
                    supplies[i] = this.fixedShare;
                    consumersSupplied++;
                }
            }

            // Build the list of consumers that didn't get resources this round
            for (int i = 0; i < this.consumerEdges.size(); i++) {
                if (supplies[i] == 0.0) {
                    currentNotSuppliedList.add(i);
                }
            }

            // Update the tracking array for next round
            this.notSuppliedConsumers =
                    currentNotSuppliedList.stream().mapToInt(Integer::intValue).toArray();
        }

        return supplies;
    }

    @Override
    // index of not supplied consumers also need to be updated
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

        // Decrease the index of not supplied consumers
        for (int i = 0; i < this.notSuppliedConsumers.length; i++) {
            if (this.notSuppliedConsumers[i] > idx) {
                this.notSuppliedConsumers[i]--;
            }
        }

        this.outgoingDemandUpdateNeeded = true;
        this.invalidate();
    }
}
