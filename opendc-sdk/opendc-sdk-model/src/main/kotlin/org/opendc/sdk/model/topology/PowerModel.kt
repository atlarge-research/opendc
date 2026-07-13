/*
 * Copyright (c) 2025 AtLarge Research
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

package org.opendc.sdk.model.topology

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.opendc.common.units.Power
import org.opendc.sdk.model.validation.Validatable
import org.opendc.sdk.model.validation.ValidationIssue

/** The functional shape relating utilization to power draw. */
@Serializable
public enum class PowerModelType {
    @SerialName("constant")
    CONSTANT,

    @SerialName("linear")
    LINEAR,

    @SerialName("square")
    SQUARE,

    @SerialName("cubic")
    CUBIC,

    @SerialName("sqrt")
    SQRT,

    @SerialName("mse")
    MSE,

    @SerialName("asymptotic")
    ASYMPTOTIC,
}

/**
 * A power-consumption model for a component.
 *
 * @property type Shape of the utilization-to-power curve.
 * @property maxPower Power draw at full utilization.
 * @property idlePower Power draw at zero utilization.
 * @property power Reference power used by [PowerModelType.CONSTANT].
 * @property calibrationFactor Multiplier applied to the modelled power draw, used by [PowerModelType.MSE].
 * @property asymUtil Asymptotic utilization parameter, used by [PowerModelType.ASYMPTOTIC].
 * @property dvfs Whether dynamic voltage and frequency scaling is modelled, used by [PowerModelType.ASYMPTOTIC].
 */
@Serializable
public data class PowerModel(
    public val type: PowerModelType = PowerModelType.LINEAR,
    public val maxPower: Power,
    public val idlePower: Power,
    public val power: Power = Power.ofWatts(400),
    public val calibrationFactor: Double = 1.0,
    public val asymUtil: Double = 0.0,
    public val dvfs: Boolean = true,
) : Validatable {
    override fun validate(): List<ValidationIssue> =
        buildList {
            if (maxPower < idlePower) add(ValidationIssue("maxPower", "must be >= idlePower"))
            if (calibrationFactor <= 0.0) add(ValidationIssue("calibrationFactor", "must be > 0"))
        }

    public companion object {
        /** A sensible default power model. */
        public val DEFAULT: PowerModel =
            PowerModel(PowerModelType.LINEAR, Power.ofWatts(400), Power.ofWatts(200), Power.ofWatts(350))
    }
}
