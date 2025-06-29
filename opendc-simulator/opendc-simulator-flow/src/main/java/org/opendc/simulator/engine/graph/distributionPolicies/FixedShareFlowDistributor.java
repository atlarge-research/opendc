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
 * A {@link FlowDistributor} that distributes the supply to the consumers based on a fixed share ratio.
 * The share ratio is defined as a percentage of the capacity of the suppliers.
 * <p>
 * This distributor is useful when you want to ensure that each consumer receives a fixed share of the total supply,
 * regardless of their individual demand.
 */
public class FixedShareFlowDistributor extends FlowDistributor {

    private boolean overloaded = false;
    private final double shareRatio;
    private final double totalCapacity;
    private double[] previouslySuppliedConsumers = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Basic Getters and Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public double getShareRatio() {
        return shareRatio;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public FixedShareFlowDistributor(FlowEngine engine, double shareRatio) {
        super(engine);
        this.shareRatio = shareRatio;
        this.totalCapacity = this.supplierEdges.values().stream()
                .mapToDouble(FlowEdge::getCapacity)
                .sum();
        this.previouslySuppliedConsumers = new double[this.consumerEdges.size()];
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Distribution Logic
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Updates the outgoing demand for each supplier edge based on the total capacity, the number of suppliers, and the share ratio.
     * The demand is fixed.
     * This method is called when the outgoing demand needs to be updated.
     */
    protected void updateOutgoingDemand() {
        // equally distribute the demand to all suppliers
        double demandShare = (this.totalCapacity / this.supplierEdges.size()) * this.shareRatio;
        for (FlowEdge supplierEdge : this.supplierEdges.values()) {
            this.pushOutgoingDemand(supplierEdge, demandShare);
        }

        this.outgoingDemandUpdateNeeded = false;

        this.invalidate();
    }

    protected void updateOutgoingSupplies() {

        this.updatedDemands.clear();
    }

    private record Demand(int idx, double value) {}

    public double[] distributeSupply(ArrayList<Double> demands, ArrayList<Double> currentSupply, double totalSupply) {

        return new double[0];
    }
}
