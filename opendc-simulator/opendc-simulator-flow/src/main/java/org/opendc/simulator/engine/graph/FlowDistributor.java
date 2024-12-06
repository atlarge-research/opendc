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

package org.opendc.simulator.engine.graph;

import java.util.ArrayList;
import java.util.Arrays;

public class FlowDistributor extends FlowNode implements FlowSupplier, FlowConsumer {
    private final ArrayList<FlowEdge> consumerEdges = new ArrayList<>();
    private FlowEdge supplierEdge;

    private final ArrayList<Double> demands = new ArrayList<>(); // What is demanded by the consumers
    private final ArrayList<Double> supplies = new ArrayList<>(); // What is supplied to the consumers

    private double totalDemand; // The total demand of all the consumers
    private double totalSupply; // The total supply from the supplier

    private boolean overLoaded = false;
    private int currentConsumerIdx = -1;

    private double capacity; // What is the max capacity

    public FlowDistributor(FlowGraph graph) {
        super(graph);
    }

    public double getTotalDemand() {
        return totalDemand;
    }

    public double getTotalSupply() {
        return totalSupply;
    }

    public double getCapacity() {
        return capacity;
    }

    public long onUpdate(long now) {

        return Long.MAX_VALUE;
    }

    private void distributeSupply() {
        // if supply >= demand -> push supplies to all tasks
        if (this.totalSupply > this.totalDemand) {

            // If this came from a state of overload, provide all consumers with their demand
            if (this.overLoaded) {
                for (int idx = 0; idx < this.consumerEdges.size(); idx++) {
                    this.pushSupply(this.consumerEdges.get(idx), this.demands.get(idx));
                }
            }

            if (this.currentConsumerIdx != -1) {
                this.pushSupply(
                        this.consumerEdges.get(this.currentConsumerIdx), this.demands.get(this.currentConsumerIdx));
                this.currentConsumerIdx = -1;
            }

            this.overLoaded = false;
        }

        // if supply < demand -> distribute the supply over all consumers
        else {
            this.overLoaded = true;
            double[] supplies = redistributeSupply(this.demands, this.totalSupply);

            for (int idx = 0; idx < this.consumerEdges.size(); idx++) {
                this.pushSupply(this.consumerEdges.get(idx), supplies[idx]);
            }
        }
    }

    private record Demand(int idx, double value) {}

    /**
     * Distributed the available supply over the different demands.
     * The supply is distributed using MaxMin Fairness.
     *
     * TODO: Move this outside of the Distributor so we can easily add different redistribution methods
     */
    private static double[] redistributeSupply(ArrayList<Double> demands, double totalSupply) {
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

        double availableCapacity = totalSupply; // totalSupply

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

        // Return the used capacity
        return supplies;
    }

    /**
     * Add a new consumer.
     * Set its demand and supply to 0.0
     */
    @Override
    public void addConsumerEdge(FlowEdge consumerEdge) {
        consumerEdge.setConsumerIndex(this.consumerEdges.size());

        this.consumerEdges.add(consumerEdge);
        this.demands.add(0.0);
        this.supplies.add(0.0);
    }

    @Override
    public void addSupplierEdge(FlowEdge supplierEdge) {
        this.supplierEdge = supplierEdge;
        this.capacity = supplierEdge.getCapacity();
        this.totalSupply = 0;
    }

    @Override
    public void removeConsumerEdge(FlowEdge consumerEdge) {
        int idx = consumerEdge.getConsumerIndex();

        if (idx == -1) {
            return;
        }

        this.totalDemand -= consumerEdge.getDemand();

        this.consumerEdges.remove(idx);
        this.demands.remove(idx);
        this.supplies.remove(idx);

        // update the consumer index for all consumerEdges higher than this.
        for (int i = idx; i < this.consumerEdges.size(); i++) {
            this.consumerEdges.get(i).setConsumerIndex(i);
        }

        this.currentConsumerIdx = -1;

        if (this.overLoaded) {
            this.distributeSupply();
        }

        this.pushDemand(this.supplierEdge, this.totalDemand);
    }

    @Override
    public void removeSupplierEdge(FlowEdge supplierEdge) {
        this.supplierEdge = null;
        this.capacity = 0;
        this.totalSupply = 0;
    }

    @Override
    public void handleDemand(FlowEdge consumerEdge, double newDemand) {
        int idx = consumerEdge.getConsumerIndex();

        this.currentConsumerIdx = idx;

        if (idx == -1) {
            System.out.println("Error (FlowDistributor): Demand pushed by an unknown consumer");
            return;
        }

        // Update the total demand (This is cheaper than summing over all demands)
        double prevDemand = demands.get(idx);

        demands.set(idx, newDemand);
        this.totalDemand += (newDemand - prevDemand);

        if (overLoaded) {
            distributeSupply();
        }

        // Send new totalDemand to CPU
        // TODO: Look at what happens if total demand is not changed (if total demand is higher than totalSupply)
        this.pushDemand(this.supplierEdge, this.totalDemand);
    }

    @Override
    public void handleSupply(FlowEdge supplierEdge, double newSupply) {
        this.totalSupply = newSupply;

        this.distributeSupply();
    }

    @Override
    public void pushDemand(FlowEdge supplierEdge, double newDemand) {
        this.supplierEdge.pushDemand(newDemand);
    }

    @Override
    public void pushSupply(FlowEdge consumerEdge, double newSupply) {
        int idx = consumerEdge.getConsumerIndex();

        if (idx == -1) {
            System.out.println("Error (FlowDistributor): pushing supply to an unknown consumer");
        }

        if (supplies.get(idx) == newSupply) {
            return;
        }

        supplies.set(idx, newSupply);
        consumerEdge.pushSupply(newSupply);
    }
}
