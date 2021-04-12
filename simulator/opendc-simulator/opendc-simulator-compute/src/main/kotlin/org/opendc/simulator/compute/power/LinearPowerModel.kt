package org.opendc.simulator.compute.power

/**
 * The linear power model partially adapted from CloudSim.
 *
 * @param maxPower The maximum power draw of the server in W.
 * @param staticPower The power draw of the server in halting state in W.
 */
public class LinearPowerModel(private val maxPower: Double, private val staticPower: Double) : PowerModel {
    /**
     * The linear interpolation factor of the model.
     */
    private val factor: Double = (maxPower - staticPower) / 100

    public override fun computePower(utilization: Double): Double {
        return staticPower + factor * utilization * 100
    }

    override fun toString(): String = "LinearPowerModel[max=$maxPower,static=$staticPower]"
}
