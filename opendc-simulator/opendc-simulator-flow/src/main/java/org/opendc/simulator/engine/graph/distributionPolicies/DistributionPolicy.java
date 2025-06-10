package org.opendc.simulator.engine.graph.distributionPolicies;

import java.util.ArrayList;

public interface DistributionPolicy {
    double[] distributeSupply(ArrayList<Double> supply, double currentSupply);
}
