package org.opendc.simulator.compute.power

import kotlin.math.pow

/**
 * The power model that minimizes the mean squared error (MSE)
 * to the actual power measurement by tuning the calibration parameter.
 * @see <a href="https://dl.acm.org/doi/abs/10.1145/1273440.1250665">
 *     Fan et al., Power provisioning for a warehouse-sized computer, ACM SIGARCH'07</a>
 *
 * @param maxPower The maximum power draw of the server in W.
 * @param idlePower The power draw of the server at its lowest utilization level in W.
 * @param calibrationParam The parameter set to minimize the MSE.
 */
public class MsePowerModel(
    private val maxPower: Double,
    private val idlePower: Double,
    private val calibrationParam: Double,
) : PowerModel {
    private val factor: Double = (maxPower - idlePower) / 100

    public override fun computePower(utilization: Double): Double {
        return idlePower + factor * (2 * utilization - utilization.pow(calibrationParam)) * 100
    }

    override fun toString(): String = "MsePowerModel[max=$maxPower,idle=$idlePower,MSE_param=$calibrationParam]"
}
