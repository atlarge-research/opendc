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

import org.opendc.simulator.engine.engine.FlowEngine;
import org.opendc.simulator.engine.graph.FlowDistributor;
import org.opendc.simulator.engine.graph.FlowEdge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class FixedShareFlowDistributor extends FlowDistributor {

    private boolean overloaded = false;

    public FixedShareFlowDistributor(FlowEngine engine) {
        super(engine);
    }

    protected void updateOutgoingDemand() {
        // equally distribute the demand to all suppliers
        for (FlowEdge supplierEdge : this.supplierEdges.values()) {
            this.pushOutgoingDemand(supplierEdge, this.totalIncomingDemand / this.supplierEdges.size());
            // alternatively a relative share could be used, based on capacity minus current incoming supply
            //            this.pushOutgoingDemand(supplierEdge, this.totalIncomingDemand * (supplierEdge.getCapacity() -
            // currentIncomingSupplies.get(idx) / supplierEdges.size()));
        }

        this.outgoingDemandUpdateNeeded = false;

        this.invalidate();
    }

    // TODO: This should probably be moved to the distribution strategy
    protected void updateOutgoingSupplies() {

        // If the demand is higher than the current supply, the system is overloaded.
        // The available supply is distributed based on the current distribution function.
        if (this.totalIncomingDemand > this.totalIncomingSupply) {
            this.overloaded = true;

            double[] supplies = this.distributeSupply(
                    this.incomingDemands,
                    new ArrayList<>(this.currentIncomingSupplies.values()),
                    this.totalIncomingSupply);

            for (int idx = 0; idx < this.consumerEdges.size(); idx++) {
                this.pushOutgoingSupply(this.consumerEdges.get(idx), supplies[idx], this.getConsumerResourceType());
            }

        } else {

            // If the distributor was overloaded before, but is not anymore:
            //      provide all consumers with their demand
            if (this.overloaded) {
                for (int idx = 0; idx < this.consumerEdges.size(); idx++) {
                    if (!Objects.equals(this.outgoingSupplies.get(idx), this.incomingDemands.get(idx))) {
                        this.pushOutgoingSupply(
                                this.consumerEdges.get(idx),
                                this.incomingDemands.get(idx),
                                this.getConsumerResourceType());
                    }
                }
                this.overloaded = false;
            }

            // Update the supplies of the consumers that changed their demand in the current cycle
            else {
                for (int idx : this.updatedDemands) {
                    this.pushOutgoingSupply(
                            this.consumerEdges.get(idx), this.incomingDemands.get(idx), this.getConsumerResourceType());
                }
            }
        }

        this.updatedDemands.clear();
    }

    private record Demand(int idx, double value) {}

    public double[] distributeSupply(ArrayList<Double> demands, ArrayList<Double> currentSupply, double totalSupply) {
        int inputSize = demands.size();

        final double[] supplies = new double[inputSize];
        final Demand[] tempDemands = new Demand[inputSize];

        for (int i = 0; i < inputSize; i++) {
            tempDemands[i] = new Demand(i, demands.get(i));
        }

        Arrays.sort(tempDemands, (o1, o2) -> {
            Double i1 = o1.value;
            Double i2 = o2.value;
            return i1.compareTo(i2);
        });

        double availableCapacity = totalSupply;

        for (int i = 0; i < inputSize; i++) {
            double d = tempDemands[i].value;

            if (d == 0.0) {
                continue;
            }

            double availableShare = availableCapacity / (inputSize - i);
            double r = Math.min(d, availableShare);

            int idx = tempDemands[i].idx;
            supplies[idx] = r; // Update the rates
            availableCapacity -= r;
        }

        return supplies;
    }
}
