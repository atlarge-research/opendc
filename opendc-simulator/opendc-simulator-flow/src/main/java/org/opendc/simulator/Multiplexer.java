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

package org.opendc.simulator;

import java.util.ArrayList;
import java.util.Arrays;
import org.opendc.simulator.engine.FlowConsumer;
import org.opendc.simulator.engine.FlowEdge;
import org.opendc.simulator.engine.FlowGraph;
import org.opendc.simulator.engine.FlowNode;
import org.opendc.simulator.engine.FlowSupplier;

public class Multiplexer extends FlowNode implements FlowSupplier, FlowConsumer {
    private final ArrayList<FlowEdge> consumerEdges = new ArrayList<>();
    private FlowEdge supplierEdge;

    private final ArrayList<Double> demands = new ArrayList<>(); // What is demanded by the consumers
    private final ArrayList<Double> supplies = new ArrayList<>(); // What is supplied to the consumers

    private double totalDemand; // The total demand of all the consumers
    private double totalSupply; // The total supply from the supplier
    private double capacity; // What is the max capacity

    public Multiplexer(FlowGraph graph) {
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

        if (this.totalDemand > this.capacity) {
            redistributeSupply(this.consumerEdges, this.supplies, this.capacity);
        } else {
            for (int i = 0; i < this.demands.size(); i++) {
                this.supplies.set(i, this.demands.get(i));
            }
        }

        double totalSupply = 0;
        for (int i = 0; i < this.consumerEdges.size(); i++) {
            this.pushSupply(this.consumerEdges.get(i), this.supplies.get(i));
            totalSupply += this.supplies.get(i);
        }

        // Only update supplier if supply has changed
        if (this.totalSupply != totalSupply) {
            this.totalSupply = totalSupply;

            pushDemand(this.supplierEdge, this.totalSupply);
        }

        return Long.MAX_VALUE;
    }

    private static double redistributeSupply(
            ArrayList<FlowEdge> consumerEdges, ArrayList<Double> supplies, double capacity) {
        final long[] consumers = new long[consumerEdges.size()];

        for (int i = 0; i < consumers.length; i++) {
            FlowEdge consumer = consumerEdges.get(i);

            if (consumer == null) {
                break;
            }

            consumers[i] = (Double.doubleToRawLongBits(consumer.getDemand()) << 32) | (i & 0xFFFFFFFFL);
        }
        Arrays.sort(consumers);

        double availableCapacity = capacity;
        int inputSize = consumers.length;

        for (int i = 0; i < inputSize; i++) {
            long v = consumers[i];
            int slot = (int) v;
            double d = Double.longBitsToDouble((int) (v >> 32));

            if (d == 0.0) {
                continue;
            }

            double availableShare = availableCapacity / (inputSize - i);
            double r = Math.min(d, availableShare);

            supplies.set(slot, r); // Update the rates
            availableCapacity -= r;
        }

        // Return the used capacity
        return capacity - availableCapacity;
    }

    /**
     * Add a new consumer.
     * Set its demand and supply to 0.0
     */
    @Override
    public void addConsumerEdge(FlowEdge consumerEdge) {
        this.consumerEdges.add(consumerEdge);
        this.demands.add(0.0);
        this.supplies.add(0.0);

        this.invalidate();
    }

    @Override
    public void addSupplierEdge(FlowEdge supplierEdge) {
        this.supplierEdge = supplierEdge;
        this.capacity = supplierEdge.getCapacity();
        this.totalSupply = 0;

        this.invalidate();
    }

    @Override
    public void removeConsumerEdge(FlowEdge consumerEdge) {
        int idx = this.consumerEdges.indexOf(consumerEdge);

        if (idx == -1) {
            return;
        }

        this.totalDemand -= consumerEdge.getDemand();

        this.consumerEdges.remove(idx);
        this.demands.remove(idx);
        this.supplies.remove(idx);

        this.invalidate();
    }

    @Override
    public void removeSupplierEdge(FlowEdge supplierEdge) {
        this.supplierEdge = null;
        this.capacity = 0;
        this.totalSupply = 0;
    }

    @Override
    public void handleDemand(FlowEdge consumerEdge, double newDemand) {
        int idx = consumerEdges.indexOf(consumerEdge);

        if (idx == -1) {
            System.out.println("Error (Multiplexer): Demand pushed by an unknown consumer");
            return;
        }

        double prevDemand = demands.get(idx);
        demands.set(idx, newDemand);

        this.totalDemand += (newDemand - prevDemand);

        if (this.totalDemand <= this.capacity) {

            this.totalSupply = this.totalDemand;
            this.pushDemand(this.supplierEdge, this.totalSupply);

            this.pushSupply(consumerEdge, newDemand);
        }
        // TODO: add behaviour if capacity is reached
    }

    @Override
    public void handleSupply(FlowEdge supplierEdge, double newSupply) {}

    @Override
    public void pushDemand(FlowEdge supplierEdge, double newDemand) {
        this.supplierEdge.pushDemand(newDemand);
    }

    @Override
    public void pushSupply(FlowEdge consumerEdge, double newSupply) {
        int idx = consumerEdges.indexOf(consumerEdge);

        if (idx == -1) {
            System.out.println("Error (Multiplexer): pushing supply to an unknown consumer");
        }

        if (supplies.get(idx) == newSupply) {
            return;
        }

        supplies.set(idx, newSupply);
        consumerEdge.pushSupply(newSupply);
    }
}
