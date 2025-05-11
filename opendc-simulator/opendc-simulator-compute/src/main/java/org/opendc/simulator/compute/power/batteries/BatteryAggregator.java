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

package org.opendc.simulator.compute.power.batteries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jdk.jshell.spi.ExecutionControl;
import org.opendc.common.ResourceType;
import org.opendc.simulator.engine.engine.FlowEngine;
import org.opendc.simulator.engine.graph.FlowConsumer;
import org.opendc.simulator.engine.graph.FlowDistributor;
import org.opendc.simulator.engine.graph.FlowEdge;
import org.opendc.simulator.engine.graph.FlowNode;
import org.opendc.simulator.engine.graph.FlowSupplier;

public class BatteryAggregator extends FlowNode implements FlowConsumer, FlowSupplier {

    private FlowEdge batteryEdge;
    private FlowEdge powerSourceEdge;
    private FlowEdge hostEdge;

    private PowerSourceType powerSourceType = PowerSourceType.PowerSource;

    private double incomingDemand;
    private double outgoingSupply;

    private double incomingSupply;

    private final ArrayList<Double> incomingSupplies = new ArrayList<>(Arrays.asList(0.0, 0.0));
    private final ArrayList<Double> outgoingDemands = new ArrayList<>(Arrays.asList(0.0, 0.0));

    private boolean outgoingDemandUpdateNeeded = false;

    /**
     * Construct a new {@link FlowNode} instance.
     *
     * @param engine The {@link FlowEngine} this node belongs to.
     */
    public BatteryAggregator(FlowEngine engine, SimBattery battery, FlowDistributor powerSourceDistributor) {
        super(engine);

        this.powerSourceEdge = new FlowEdge(this, powerSourceDistributor);
        this.powerSourceEdge.setSupplierIndex(0);

        this.batteryEdge = new FlowEdge(this, battery);
        this.batteryEdge.setSupplierIndex(1);
    }

    public void close() {
        if (this.batteryEdge == null) {
            return;
        }

        this.batteryEdge = null;
        this.powerSourceEdge = null;

        this.closeNode();
    }

    @Override
    public long onUpdate(long now) {

        if (this.outgoingDemandUpdateNeeded) {

            if (this.powerSourceType == PowerSourceType.PowerSource) {
                this.pushOutgoingDemand(this.batteryEdge, 0.0f);
                this.pushOutgoingDemand(this.powerSourceEdge, this.incomingDemand);
            }

            if (this.powerSourceType == PowerSourceType.Battery) {
                this.pushOutgoingDemand(this.powerSourceEdge, 0.0f);
                this.pushOutgoingDemand(this.batteryEdge, this.incomingDemand);
            }

            this.outgoingDemandUpdateNeeded = false;

            this.invalidate();

            return Long.MAX_VALUE;
        }

        if (this.hostEdge != null) {
            this.pushOutgoingSupply(this.hostEdge, this.incomingSupply);
        }

        return Long.MAX_VALUE;
    }

    @Override
    public void handleIncomingDemand(FlowEdge consumerEdge, double newDemand) {
        this.incomingDemand = newDemand;

        this.outgoingDemandUpdateNeeded = true;
        this.invalidate();
    }

    @Override
    public void handleIncomingSupply(FlowEdge supplierEdge, double newSupply) {
        int supplier_id = supplierEdge.getSupplierIndex();

        this.incomingSupply += newSupply - this.incomingSupplies.get(supplier_id);

        this.incomingSupplies.set(supplier_id, newSupply);

        this.invalidate();
    }

    @Override
    public void pushOutgoingDemand(FlowEdge supplierEdge, double newDemand) {
        supplierEdge.pushDemand(newDemand);
    }

    @Override
    public void addSupplierEdge(FlowEdge supplierEdge) {}

    @Override
    public void removeSupplierEdge(FlowEdge supplierEdge) {
        this.close();
    }

    @Override
    public void pushOutgoingSupply(FlowEdge consumerEdge, double newSupply) {
        consumerEdge.pushSupply(newSupply);
    }

    @Override
    public void addConsumerEdge(FlowEdge consumerEdge) {
        this.hostEdge = consumerEdge;
    }

    @Override
    public void removeConsumerEdge(FlowEdge consumerEdge) {
        this.close();
    }

    public PowerSourceType getPowerSourceType() {
        return powerSourceType;
    }

    public void setPowerSourceType(PowerSourceType newPowerSourceType) {
        if (this.powerSourceType == newPowerSourceType) {
            return;
        }

        this.powerSourceType = newPowerSourceType;

        this.outgoingDemandUpdateNeeded = true;

        this.invalidate();
    }

    @Override
    public double getCapacity() {
        return 0;
    }

    @Override
    public Map<FlowEdge.NodeType, List<FlowEdge>> getConnectedEdges() {
        List<FlowEdge> consumingEdges = new ArrayList<>();
        if (this.powerSourceEdge != null) {
            consumingEdges.add(this.batteryEdge);
        }
        if (this.batteryEdge != null) {
            consumingEdges.add(this.powerSourceEdge);
        }

        List<FlowEdge> supplyingEdges = this.hostEdge != null ? List.of(this.hostEdge) : List.of();

        return Map.of(
                FlowEdge.NodeType.CONSUMING, consumingEdges,
                FlowEdge.NodeType.SUPPLYING, supplyingEdges);
    }

    @Override
    public ResourceType getResourceType(){
        return ResourceType.AUXILIARY;
    }
}
