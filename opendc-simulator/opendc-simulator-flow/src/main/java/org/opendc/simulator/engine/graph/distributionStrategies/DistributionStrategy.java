package org.opendc.simulator.engine.graph.distributionStrategies;

import java.util.ArrayList;

public interface DistributionStrategy {
    double[] distributeSupply(ArrayList<Double> supply, double currentSupply);
}
