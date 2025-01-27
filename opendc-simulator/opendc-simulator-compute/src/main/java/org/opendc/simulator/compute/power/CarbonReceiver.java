package org.opendc.simulator.compute.power;

public interface CarbonReceiver {

    public void updateCarbonIntensity(double carbonIntensity);

    public void setCarbonModel(CarbonModel carbonModel);

    public void removeCarbonModel(CarbonModel carbonModel);
}
