package org.opendc.simulator.engine.graph.distributionStrategies;

import org.opendc.simulator.engine.graph.FlowDistributor;
//import org.opendc.simulator.engine.graph.FlowDistributor.Demand;
import java.util.ArrayList;
import java.util.Arrays;

public class MaxMinFairnessStrategy implements DistributionStrategy{
    private record Demand(int idx, double value) {}

    @Override
    public double[] distributeSupply(ArrayList<Double> demands, double currentSupply) {
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

        double availableCapacity = currentSupply; // totalSupply

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
