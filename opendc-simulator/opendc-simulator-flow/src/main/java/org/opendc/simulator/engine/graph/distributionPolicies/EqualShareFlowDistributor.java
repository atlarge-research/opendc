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
import java.util.Arrays;
import org.opendc.simulator.engine.engine.FlowEngine;
import org.opendc.simulator.engine.graph.FlowDistributor;

public class EqualShareFlowDistributor extends FlowDistributor {

    public EqualShareFlowDistributor(FlowEngine engine) {
        super(engine);
    }

    /**
     * Updates the outgoing demand for each supplier edge based on the total capacity.
     * The demand is equally distributed among all suppliers.
     * This method is called when the outgoing demand needs to be updated.
     */
    @Override
    protected void updateOutgoingDemand() {
        //        for (var supplierEdge : this.supplierEdges.values()) {
        //            this.pushOutgoingDemand(supplierEdge, totalIncomingDemand / this.supplierEdges.size());
        //        }
        //        this.outgoingDemandUpdateNeeded = false;

        double equalShare = this.capacity / this.supplierEdges.size();

        for (var supplierEdge : this.supplierEdges.values()) {
            this.pushOutgoingDemand(supplierEdge, equalShare);
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
        //        for (var consumerEdge : this.consumerEdges) {
        //            this.pushOutgoingSupply(consumerEdge, totalIncomingSupply / this.consumerEdges.size());
        //        }
        // Equal distribution regardless of what each consumer actually needs
        //        double equalShare = totalIncomingSupply / this.consumerEdges.size();
        double[] equalShare = distributeSupply(incomingDemands, outgoingSupplies, this.capacity);

        for (var consumerEdge : this.consumerEdges) {
            this.pushOutgoingSupply(consumerEdge, equalShare[consumerEdge.getConsumerIndex()]);
        }
    }

    @Override
    public double[] distributeSupply(ArrayList<Double> demands, ArrayList<Double> currentSupply, double totalSupply) {
        int numConsumers = demands.size();
        double[] allocation = new double[numConsumers];
        double equalShare = totalSupply / numConsumers;

        // Equal share regardless of individual demands
        Arrays.fill(allocation, equalShare);

        return allocation;
    }
}
