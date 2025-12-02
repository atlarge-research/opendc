package org.opendc.simulator.compute.costmodel;

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
public class CostModel extends FlowNode {

    private final long startTime; // The absolute timestamp on which the workload started

    //TODO test var
    private double test = 0f;
    private double energyCostPerKWH = 0f;

    private final List<EnergyCostFragment> fragments;
    private int fragment_index;
    private EnergyCostFragment current_fragment;

    public CostModel(FlowEngine engine, List<EnergyCostFragment> energyCostFragmentsList, long startTime) {
        super(engine);

        this.startTime = startTime;
        this.fragments = energyCostFragmentsList;

        this.fragment_index = 0;
        this.current_fragment = this.fragments.get(this.fragment_index);
    }

    public void close() {
        this.closeNode();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Basic Getters and Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public double getEnergyCostPerKWH() {
        return energyCostPerKWH;
    }

    public double getTest() {
        return test;
    }


    @Override
    public long onUpdate(long now) {
        long absolute_time = getAbsoluteTime(now);

        //TODO JUST like in carbon model we need to fin the correct fragment, im testing with a singular fragment
        // spanning all of time that can be represented in a long, so there is no need yet

        // TODO FIgure out if this is a reasonable way of sending the energy cost towards output
        updateEnergyCost(current_fragment.getEnergyPrice());

        // Check if the current fragment is still the correct fragment,
        // Otherwise, find the correct fragment.
//        if ((absolute_time < current_fragment.getStartTime()) || (absolute_time >= current_fragment.getEndTime())) {
//            this.findCorrectFragment(absolute_time);
//
//            pushCarbonIntensity(current_fragment.getCarbonIntensity());
//        }

        //We make the simulator aware of our fragments timeline, so that we are called at the end of each fragment and
        //can update our own bookkeeping based on the current state of the simulator
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
        test = (test + 1) % 100; // TODO bogus

    }

    //TODO carbon send it to a bunch of recievers, why?!
//    private void pushEnergyCost(double energyPricePER_KWH) {
//        for (CarbonReceiver receiver : this.receivers) {
//            receiver.updateEnergyCost(carbonIntensity);
//        }
//    }

    public void updateEnergyCost(double energyPricePerKWH) {
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
}
