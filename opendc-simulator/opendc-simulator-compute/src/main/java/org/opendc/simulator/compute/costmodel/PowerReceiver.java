package org.opendc.simulator.compute.costmodel;

import org.opendc.simulator.compute.power.SimPowerSource;

public interface PowerReceiver {
    public void updatePowerData(double energyUsed);

    public void setPowerSource(SimPowerSource powerSource);

    public void removePowerSource(SimPowerSource powerSource);
}



