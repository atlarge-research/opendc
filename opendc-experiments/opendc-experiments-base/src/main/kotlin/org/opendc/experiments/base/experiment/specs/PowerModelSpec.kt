/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.experiments.base.experiment.specs

import kotlinx.serialization.Serializable

/**
 * Specification describing the power model used to compute the power draw of a host.
 *
 * @property type Name of the power model to use (e.g. `"constant"`, `"linear"`, `"square"`, `"cubic"`, `"sqrt"`).
 * @property idlePower Power drawn by a host at 0% utilization, in Watts. Must not be negative.
 * @property maxPower Power drawn by a host at 100% utilization, in Watts. Must not be negative and must
 * be at least [minPower].
 * @property minPower Minimum power drawn by a host, in Watts. Must not be negative.
 * @property calibrationFactor Factor used to calibrate the power model. Must be positive.
 * @property asymUtil Utilization at which the asymptote of the power curve is placed, used by some models.
 * @property dvfs Whether Dynamic Voltage and Frequency Scaling is modelled.
 */
@Serializable
@Deprecated("Replaced by the opendc-sdk model (org.opendc.sdk.model.*); run experiments with the new opendc CLI (opendc-cli).")
public data class PowerModelSpec(
    val type: String = "constant",
    val idlePower: Double = 200.0,
    val maxPower: Double = 350.0,
    val minPower: Double = 200.0,
    val calibrationFactor: Double = 1.0,
    val asymUtil: Double = 0.0,
    val dvfs: Boolean = false,
) {
    /**
     * Validate the constraints of this power model specification.
     *
     * All violated constraints are collected and reported together, so a user fixing a power model
     * sees every problem at once instead of one per run. When any constraint is violated an
     * [InvalidPowerModelException] is thrown; otherwise this returns nothing.
     *
     * @throws InvalidPowerModelException if one or more constraints are violated.
     */
    public fun validate() {
        val errors =
            buildList {
                if (type.isBlank()) {
                    add("The power model type can not be blank")
                }
                if (idlePower < 0.0) {
                    add("The idle power can not be negative (currently idlePower=$idlePower)")
                }
                if (minPower < 0.0) {
                    add("The minimum power can not be negative (currently minPower=$minPower)")
                }
                if (maxPower < 0.0) {
                    add("The maximum power can not be negative (currently maxPower=$maxPower)")
                }
                if (maxPower < minPower) {
                    add("The maximum power can not be smaller than the minimum power (currently maxPower=$maxPower, minPower=$minPower)")
                }
                if (calibrationFactor <= 0.0) {
                    add("The calibration factor must be positive (currently calibrationFactor=$calibrationFactor)")
                }
            }

        if (errors.isNotEmpty()) {
            throw InvalidPowerModelException(errors)
        }
    }
}

/**
 * Exception thrown when a [PowerModelSpec] violates one or more of its constraints.
 *
 * @property errors The human-readable descriptions of every violated constraint.
 */
public class InvalidPowerModelException(
    public val errors: List<String>,
) : IllegalArgumentException(
        "Invalid power model specification:\n" + errors.joinToString("\n") { "  - $it" },
    )
