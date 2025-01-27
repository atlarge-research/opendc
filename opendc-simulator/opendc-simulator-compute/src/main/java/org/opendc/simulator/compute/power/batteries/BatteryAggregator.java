package org.opendc.simulator.compute.power.batteries;

import org.opendc.simulator.engine.graph.FlowConsumer;
import org.opendc.simulator.engine.graph.FlowDistributor;
import org.opendc.simulator.engine.graph.FlowEdge;
import org.opendc.simulator.engine.graph.FlowGraph;
import org.opendc.simulator.engine.graph.FlowNode;
import org.opendc.simulator.engine.graph.FlowSupplier;

import java.util.ArrayList;
import java.util.Arrays;

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
     * @param parentGraph The {@link FlowGraph} this stage belongs to.
     */
    public BatteryAggregator(FlowGraph parentGraph, SimBattery battery, FlowDistributor powerSourceDistributor) {
        super(parentGraph);

        this.powerSourceEdge = parentGraph.addEdge(this, powerSourceDistributor);
        this.powerSourceEdge.setSupplierIndex(0);
        this.batteryEdge = parentGraph.addEdge(this, battery);
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
}
