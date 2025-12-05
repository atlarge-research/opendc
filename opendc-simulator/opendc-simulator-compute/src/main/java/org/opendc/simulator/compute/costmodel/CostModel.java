package org.opendc.simulator.compute.costmodel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opendc.simulator.compute.power.SimPowerSource;
import org.opendc.simulator.engine.engine.FlowEngine;
import org.opendc.simulator.engine.graph.FlowEdge;
import org.opendc.simulator.engine.graph.FlowNode;

import java.util.List;
import java.util.Map;

/**
 * CostModel used work out the costs based on other consumers
 */

@SuppressWarnings({
    "unused",
    "FieldMayBeFinal",
    "FieldCanBeLocal"
}) // STFU WHILE TYPE THANK YOU
public class CostModel extends FlowNode implements PowerReceiver {
    private static final Logger log = LogManager.getLogger(CostModel.class); //implements PowerReceiver

    private final long startTime; // The absolute timestamp on which the workload started
    private long lastUpdate;

    private double test = 0f;
    private double energyCostPerKWH = 0f;
    private double energyConsumedAccounted = 0f;
    private double energyConsumed = 0f;
    private double energyCost = 0f;

    private final List<EnergyCostFragment> fragments;
    private int fragment_index;
    private EnergyCostFragment current_fragment;

    private SimPowerSource simPowerSource = null;

    public CostModel(FlowEngine engine, List<EnergyCostFragment> energyCostFragmentsList, long startTime) {
        super(engine);

        this.startTime = startTime;
        this.fragments = energyCostFragmentsList;
        this.lastUpdate = this.clock.millis();

        this.fragment_index = 0;
        this.current_fragment = this.fragments.get(this.fragment_index);
    }

    public void close() {
        if (this.simPowerSource != null) {
            this.simPowerSource.close();
        }

        this.closeNode();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Basic Getters and Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public double getEnergyCostPerKWH() {
        return energyCostPerKWH;
    }

    public double getEnergyCost() {
        return this.energyCost;
    }


    @Override
    public long onUpdate(long now) {
        long absolute_time = getAbsoluteTime(now);

        updateEnergyCost();

        if ((absolute_time < current_fragment.getStartTime()) || (absolute_time >= current_fragment.getEndTime())) {
            this.findCorrectFragment(absolute_time);
            updateEnergyCostPerKWH(current_fragment.getEnergyPrice());
        }



        return getRelativeTime(current_fragment.getEndTime());
    }

    public void updateCounters() {
        updateCounters(clock.millis());
    }

    public void updateCounters(long now) {
        /*
         * TODO we can update counters here based on time elapsed,
         * say we have X cost per Month, we can work out the elapsed time, and use
         * that to get the correct fraction of that cost relative to the elapsed time.
         */
        long lastUpdate = this.lastUpdate;
        this.lastUpdate = now;

        long passedTime = now - lastUpdate;
        if (passedTime > 0) {
            simPowerSource.updateCounters(now);

            updateEnergyCost();
        }

    }

    //TODO carbon send it to a bunch of recievers, why?!
//    private void pushEnergyCost(double energyPricePER_KWH) {
//        for (CarbonReceiver receiver : this.receivers) {
//            receiver.updateEnergyCost(carbonIntensity);
//        }
//    }

    private void updateEnergyCost(){
        if (this.energyConsumed > this.energyConsumedAccounted) {
            double energyConsumedUnAccounted = this.energyConsumed - this.energyConsumedAccounted;
            this.energyCost += energyConsumedUnAccounted*this.energyCostPerKWH/3600000;
            this.energyConsumedAccounted += energyConsumedUnAccounted;
        }
    }

    private void findCorrectFragment(long absoluteTime) {

        // Traverse to the previous fragment, until you reach the correct fragment
        while (absoluteTime < this.current_fragment.getStartTime()) {
            this.current_fragment = fragments.get(--this.fragment_index);
        }

        // Traverse to the next fragment, until you reach the correct fragment
        while (absoluteTime >= this.current_fragment.getEndTime()) {
            this.current_fragment = fragments.get(++this.fragment_index);
        }
    }

    public void updateEnergyCostPerKWH(double energyPricePerKWH) {
        this.updateCounters();
        this.energyCostPerKWH = energyPricePerKWH;
    }

    /**
     * Convert the given relative time to the absolute time by adding the start of workload
     */
    private long getAbsoluteTime(long time) {
        return time + startTime;
    }

    /**
     * Convert the given absolute time to the relative time by subtracting the start of workload
     */
    private long getRelativeTime(long time) {
        return time - startTime;
    }

    /*
     * TODO NICO Not sure how this shits work, little documention on src code but this function is required by our super
     */
    @Override
    public Map<FlowEdge.NodeType, List<FlowEdge>> getConnectedEdges() {
        return Map.of();
    }

    @Override
    public void updatePowerData(double energyConsumed) {
        this.energyConsumed = energyConsumed;
    }

    @Override
    public void setPowerSource(SimPowerSource powerSource) {
        this.simPowerSource = powerSource;
    }

    @Override
    public void removePowerSource(SimPowerSource powerSource) {
        this.simPowerSource = null;
    }
}
