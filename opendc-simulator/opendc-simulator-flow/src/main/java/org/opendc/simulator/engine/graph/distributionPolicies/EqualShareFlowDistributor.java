package org.opendc.simulator.engine.graph.distributionPolicies;

import org.opendc.simulator.engine.engine.FlowEngine;
import org.opendc.simulator.engine.graph.FlowDistributor;

import java.util.ArrayList;

public class EqualShareFlowDistributor extends FlowDistributor {

    public EqualShareFlowDistributor(FlowEngine engine) {
        super(engine);
    }

    @Override
    protected void updateOutgoingDemand() {

    }

    @Override
    protected void updateOutgoingSupplies() {

    }

    @Override
    public double[] distributeSupply(ArrayList<Double> demands, ArrayList<Double> currentSupply, double totalSupply) {
        return new double[0];
    }
}
