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

/** Describes how a component's power draw relates to its utilization. */
@Serializable
public sealed interface PowerModelSpec : Validatable {
    override fun validate(): List<ValidationIssue> = emptyList()

    public companion object {
        /** A sensible default power model. */
        public val DEFAULT: PowerModelSpec = LinearPowerModelSpec(maxPower = Power.ofWatts(400), idlePower = Power.ofWatts(200))
    }
}

/**
 * A component that draws the same power regardless of its utilization.
 *
 * @property power Power draw at every utilization level.
 */
@Serializable
@SerialName("constant")
public data class ConstantPowerModelSpec(
    public val power: Power,
) : PowerModelSpec

/**
 * A power model whose draw rises from [idlePower] at zero utilization to [maxPower] at full utilization;
 * its subtypes differ only in the shape of the curve between the two.
 */
@Serializable
public sealed interface MaxIdlePowerModelSpec : PowerModelSpec {
    /** Power draw at full utilization. */
    public val maxPower: Power

    /** Power draw at zero utilization. */
    public val idlePower: Power

    override fun validate(): List<ValidationIssue> =
        if (maxPower < idlePower) listOf(ValidationIssue("maxPower", "must be >= idlePower")) else emptyList()
}

/** Power draw grows linearly with utilization. */
@Serializable
@SerialName("linear")
public data class LinearPowerModelSpec(
    override val maxPower: Power,
    override val idlePower: Power,
) : MaxIdlePowerModelSpec

/** Power draw grows with the square of utilization. */
@Serializable
@SerialName("square")
public data class SquarePowerModelSpec(
    override val maxPower: Power,
    override val idlePower: Power,
) : MaxIdlePowerModelSpec

/** Power draw grows with the cube of utilization. */
@Serializable
@SerialName("cubic")
public data class CubicPowerModelSpec(
    override val maxPower: Power,
    override val idlePower: Power,
) : MaxIdlePowerModelSpec

/** Power draw grows with the square root of utilization. */
@Serializable
@SerialName("sqrt")
public data class SqrtPowerModelSpec(
    override val maxPower: Power,
    override val idlePower: Power,
) : MaxIdlePowerModelSpec

/**
 * Power draw fitted to measurements by minimizing the mean squared error, after Fan et al., "Power
 * provisioning for a warehouse-sized computer" (ISCA 2007).
 *
 * @property calibrationFactor Exponent tuned to minimize the error against measured power draw.
 */
@Serializable
@SerialName("mse")
public data class MsePowerModelSpec(
    override val maxPower: Power,
    override val idlePower: Power,
    public val calibrationFactor: Double,
) : MaxIdlePowerModelSpec {
    override fun validate(): List<ValidationIssue> =
        buildList {
            addAll(super.validate())
            if (calibrationFactor <= 0.0) add(ValidationIssue("calibrationFactor", "must be > 0"))
        }
}

/**
 * Power draw that approaches linear growth beyond [asymUtil], adapted from GreenCloud.
 *
 * @property asymUtil Utilization at which power draw becomes close to linear in the offered load,
 *  typically in [0.2, 0.5].
 * @property dvfs Whether dynamic voltage and frequency scaling is modelled.
 */
@Serializable
@SerialName("asymptotic")
public data class AsymptoticPowerModelSpec(
    override val maxPower: Power,
    override val idlePower: Power,
    public val asymUtil: Double,
    public val dvfs: Boolean = true,
) : MaxIdlePowerModelSpec {
    override fun validate(): List<ValidationIssue> =
        buildList {
            addAll(super.validate())
            if (asymUtil <= 0.0) add(ValidationIssue("asymUtil", "must be > 0"))
        }
}
