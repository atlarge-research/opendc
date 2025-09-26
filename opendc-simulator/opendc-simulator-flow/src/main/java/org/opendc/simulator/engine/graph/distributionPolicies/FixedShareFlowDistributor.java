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
 * <a href="https://docs.nvidia.com/vgpu/knowledge-base/latest/vgpu-features.html#vgpu-schedulers">original description</a>
 * Key characteristics:
 * - Each consumer gets a fixed percentage of total GPU capacity when active
 * - Unused shares (from inactive consumers) remain unallocated, not redistributed
 * - Performance remains consistent and predictable for each consumer
 * - Share allocation is based on maximum supported consumers, not currently active ones
 */
public class FixedShareFlowDistributor extends FlowDistributor {

    private double fixedShare;
    private final double shareRatio;
    private final ArrayList<Integer> notSuppliedConsumers = new ArrayList<>();

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a Fixed Share Flow Distributor.
     *
     * @param engine The flow engine
     * @param shareRatio The fixed share ratio for each consumer (between 0.0 and 1.0)
     */
    public FixedShareFlowDistributor(FlowEngine engine, double shareRatio, int maxConsumers) {
        super(engine, maxConsumers);

        if (shareRatio <= 0 || shareRatio > 1) {
            throw new IllegalArgumentException("Share ratio must be between 0.0 and 1.0");
        }
        this.shareRatio = shareRatio;

        // Each consumer gets an equal fixed share of the total capacity
        this.fixedShare = this.shareRatio * this.capacity / this.supplierEdges.size();
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

        double[] supplies = distributeSupply(
                this.incomingDemands, new ArrayList<>(this.currentIncomingSupplies.values()), this.totalIncomingSupply);

        for (int consumerIndex : this.usedConsumerIndices) {
            this.pushOutgoingSupply(
                    this.consumerEdges[consumerIndex], supplies[consumerIndex], this.getConsumerResourceType());
        }
    }

    public double[] distributeSupply(double[] demands, ArrayList<Double> currentSupply, double totalSupply) {
        double[] supplies = new double[this.maxConsumers];

        if (this.numConsumers < this.supplierEdges.size() && this.fixedShare * this.numConsumers <= totalSupply) {

            for (int consumerIndex : this.usedConsumerIndices) {
                supplies[consumerIndex] = this.fixedShare;
            }
        } else {
            // Calculate how many consumers we can supply with available resources
            int maxConsumersToSupply = (int) Math.floor(totalSupply / this.fixedShare);
            int consumersSupplied = 0;

            // First pass: try to supply consumers that were not supplied in the previous round
            for (int consumerIndex : this.notSuppliedConsumers) {
                if (consumersSupplied >= maxConsumersToSupply) {
                    break;
                }
                supplies[consumerIndex] = this.fixedShare;
                consumersSupplied++;
            }

            this.notSuppliedConsumers.clear();
            for (int consumerIndex : this.usedConsumerIndices) {
                if (supplies[consumerIndex] == 0.0) {
                    if (consumersSupplied >= maxConsumersToSupply) {
                        this.notSuppliedConsumers.add(consumerIndex);
                    }
                    supplies[consumerIndex] = this.fixedShare;
                    consumersSupplied++;
                }
            }
        }

        return supplies;
    }

    @Override
    // index of not supplied consumers also need to be updated
    public void removeConsumerEdge(FlowEdge consumerEdge) {
        int consumerIndex = consumerEdge.getConsumerIndex();

        if (consumerIndex == -1) {
            return;
        }

        if (this.notSuppliedConsumers.contains(consumerIndex)) {
            this.notSuppliedConsumers.remove(Integer.valueOf(consumerIndex));
        }

        super.removeConsumerEdge(consumerEdge);
    }
}
