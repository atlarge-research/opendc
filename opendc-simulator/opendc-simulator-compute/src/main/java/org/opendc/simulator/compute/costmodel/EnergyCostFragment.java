package org.opendc.simulator.compute.costmodel;

 /**
 * Renamed the carbon file by hand, other than that pure copy of {@link org.opendc.simulator.compute.power.CarbonFragment}
 *
 * An object holding the energy price during a specific time frame.
 * Used by {@link CostModel}.
 */
public class EnergyCostFragment {
    private long startTime;
    private long endTime;
    private double energyPrice;

    public EnergyCostFragment(long startTime, long endTime, double energyPrice) {
        this.setStartTime(startTime);
        this.setEndTime(endTime);
        this.setEnergyPrice(energyPrice);
    }

    public double getEnergyPrice() {
        return energyPrice;
    }

    public void setEnergyPrice(double energyPrice) {
        this.energyPrice = energyPrice;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
}
