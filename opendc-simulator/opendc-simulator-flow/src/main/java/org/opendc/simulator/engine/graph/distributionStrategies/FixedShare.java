package org.opendc.simulator.engine.graph.distributionStrategies;

import java.util.ArrayList;

public class FixedShare implements DistributionStrategy {

    private int share;
    public FixedShare() {
        this.share = 1;
    }

    public FixedShare(int share) {
        this.share = share;
    }

    @Override
    public double[] distributeSupply(ArrayList<Double> supply, double currentSupply) {
        return new double[0];
    }
}
