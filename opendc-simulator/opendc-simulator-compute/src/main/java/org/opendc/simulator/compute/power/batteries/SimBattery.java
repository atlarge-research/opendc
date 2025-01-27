package org.opendc.simulator.compute.power.batteries;

import org.opendc.simulator.engine.graph.FlowConsumer;
import org.opendc.simulator.engine.graph.FlowEdge;
import org.opendc.simulator.engine.graph.FlowGraph;
import org.opendc.simulator.engine.graph.FlowNode;
import org.opendc.simulator.engine.graph.FlowSupplier;

public class SimBattery extends FlowNode implements FlowConsumer, FlowSupplier {

    private final double capacity;
    private double chargingSpeed;

    private FlowEdge distributorEdge;
    private FlowEdge aggregatorEdge;

    private BatteryState batteryState = BatteryState.Idle;

    private double charge;

    private long lastUpdate;
    private double incomingSupply;
    private double incomingDemand;

    private double outgoingDemand;
    private double outgoingSupply;

    public BatteryPolicy getBatteryPolicy() {
        return batteryPolicy;
    }

    public void setBatteryPolicy(BatteryPolicy batteryPolicy) {
        this.batteryPolicy = batteryPolicy;
    }

    private BatteryPolicy batteryPolicy;

    public BatteryState getBatteryState() {
        return batteryState;
    }

    public double getCharge() {
        return charge;
    }

    public void setCharge(double charge) {
        this.charge = charge;
    }

    @Override
    public double getCapacity() {
        return this.capacity;
    }

    public boolean isFull() {
        return (this.charge >= this.capacity);
    };

    public boolean isEmpty() {
        return (this.charge <= 0.0);
    };


    /**
     * Construct a new {@link FlowNode} instance.
     *
     * @param parentGraph The {@link FlowGraph} this stage belongs to.
     */
    public SimBattery(FlowGraph parentGraph, double capacity, double chargingSpeed) {
        super(parentGraph);
        this.capacity = capacity;
        this.chargingSpeed = chargingSpeed;
    }

    public void close() {
        if (this.distributorEdge == null) {
            return;
        }

        this.distributorEdge = null;
        this.aggregatorEdge = null;

        this.closeNode();
    }

    @Override
    public long onUpdate(long now) {

        long passedTime = now - lastUpdate;
        this.lastUpdate = now;

        if (this.batteryState == BatteryState.Idle) {
            return Long.MAX_VALUE;
        }

        this.updateCharge(passedTime);
        long remainingTime = 0L;

        if (this.batteryState == BatteryState.Charging) {
            if (this.isFull()) {
                this.batteryPolicy.invalidate();
                return Long.MAX_VALUE;
            }

            remainingTime = this.calculateRemainingTime();
        }

        if (this.batteryState == BatteryState.Discharging) {
            if (this.isEmpty()) {
                this.batteryPolicy.invalidate();
                return Long.MAX_VALUE;
            }

            this.pushOutgoingSupply(this.aggregatorEdge, this.incomingDemand);
            remainingTime = this.calculateRemainingTime();
        }

        long nextUpdate = now + remainingTime;

        if (nextUpdate < 0 ) {
            nextUpdate = Long.MAX_VALUE;
        }
        return nextUpdate;
    }

    private long calculateRemainingTime() {
        if ((this.batteryState == BatteryState.Charging) && (this.incomingSupply > 0.0)) {
            double remainingCharge = this.capacity - this.charge;
            return (long) (remainingCharge / this.incomingSupply) * 1000;
        }

        if ((this.batteryState == BatteryState.Discharging) && (this.outgoingSupply > 0.0)) {
            return (long) (this.charge / this.outgoingSupply) * 1000;
        }

        return Long.MAX_VALUE;
    }

    private void updateCharge(long passedTime) {
        if (this.batteryState == BatteryState.Charging) {
            this.charge += this.incomingSupply * (passedTime / 1000.0);
        }

        if (this.batteryState == BatteryState.Discharging) {
            this.charge -= this.outgoingSupply * (passedTime / 1000.0);
        }
    }

    public void setBatteryState(BatteryState newBatteryState) {
        if (newBatteryState == this.batteryState) {
            return;
        }

        long now = this.clock.millis();
        long passedTime = now - lastUpdate;

        updateCharge(passedTime);

        this.lastUpdate = now;

        this.batteryState = newBatteryState;

        if (this.batteryState == BatteryState.Idle) {
            this.pushOutgoingDemand(this.distributorEdge, 0.0f);
            this.pushOutgoingSupply(this.distributorEdge, 0.0f);
        }

        if (this.batteryState == BatteryState.Charging) {
            this.pushOutgoingDemand(this.distributorEdge, this.chargingSpeed);
            this.pushOutgoingSupply(this.aggregatorEdge, 0.0f);
        }

        if (this.batteryState == BatteryState.Discharging) {
            this.pushOutgoingDemand(this.distributorEdge, 0.0f);
        }

        this.invalidate();
    }

    @Override
    public void handleIncomingSupply(FlowEdge supplierEdge, double newSupply) {
        this.incomingSupply = newSupply;

        this.invalidate();
    }

    @Override
    public void pushOutgoingDemand(FlowEdge supplierEdge, double newDemand) {
        this.outgoingDemand = newDemand;

        this.distributorEdge.pushDemand(newDemand);
    }

    @Override
    public void addSupplierEdge(FlowEdge supplierEdge) {
        this.distributorEdge = supplierEdge;
    }

    @Override
    public void removeSupplierEdge(FlowEdge supplierEdge) {
        this.close();
    }

    @Override
    public void handleIncomingDemand(FlowEdge consumerEdge, double newDemand) {
        this.incomingDemand = newDemand;

        this.invalidate();
    }

    @Override
    public void pushOutgoingSupply(FlowEdge consumerEdge, double newSupply) {
        this.outgoingSupply = newSupply;

        this.aggregatorEdge.pushSupply(newSupply);
    }

    @Override
    public void addConsumerEdge(FlowEdge consumerEdge) {
        this.aggregatorEdge = consumerEdge;
    }

    @Override
    public void removeConsumerEdge(FlowEdge consumerEdge) {
        this.close();
    }
}
