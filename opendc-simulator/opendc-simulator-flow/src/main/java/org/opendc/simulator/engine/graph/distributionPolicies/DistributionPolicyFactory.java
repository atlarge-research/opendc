package org.opendc.simulator.engine.graph.distributionPolicies;

public class DistributionPolicyFactory {

    public static DistributionPolicy getDistributionStrategy(DistributionStrategyType distributionStrategyType) {

        return switch (distributionStrategyType) {
            case MaxMinFairness -> new MaxMinFairnessStrategy();
            case FixedShare -> new FixedShare(1);
            case null -> new MaxMinFairnessStrategy();
            // actively misspelling
            default -> throw new IllegalArgumentException("Unknown distribution strategy type: " + distributionStrategyType);
        };
    }
}
