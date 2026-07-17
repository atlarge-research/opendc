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

package org.opendc.sdk.model.failure

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.opendc.sdk.model.validation.Validatable
import org.opendc.sdk.model.validation.ValidationIssue

/** A statistical distribution used to sample failure timings, durations, or intensities. */
@Serializable
public sealed interface DistributionSpec : Validatable {
    override fun validate(): List<ValidationIssue> = emptyList()
}

/** A degenerate distribution that always yields the same [value]. */
@Serializable
@SerialName("constant")
public data class ConstantDistributionSpec(
    /** The fixed value produced by every sample. */
    public val value: Double,
) : DistributionSpec {
    override fun validate(): List<ValidationIssue> = if (value > 0.0) emptyList() else listOf(ValidationIssue("value", "must be > 0"))
}

/** An exponential distribution parameterized by its [mean]. */
@Serializable
@SerialName("exponential")
public data class ExponentialDistributionSpec(
    /** The mean of the distribution. */
    public val mean: Double,
) : DistributionSpec

/** A gamma distribution parameterized by [shape] and [scale]. */
@Serializable
@SerialName("gamma")
public data class GammaDistributionSpec(
    /** The shape parameter. */
    public val shape: Double,
    /** The scale parameter. */
    public val scale: Double,
) : DistributionSpec

/** A log-normal distribution parameterized by [scale] and [shape]. */
@Serializable
@SerialName("log-normal")
public data class LogNormalDistributionSpec(
    /** The scale parameter. */
    public val scale: Double,
    /** The shape parameter. */
    public val shape: Double,
) : DistributionSpec

/** A normal distribution parameterized by [mean] and standard deviation [std]. */
@Serializable
@SerialName("normal")
public data class NormalDistributionSpec(
    /** The mean of the distribution. */
    public val mean: Double,
    /** The standard deviation of the distribution. */
    public val std: Double,
) : DistributionSpec

/** A Pareto distribution parameterized by [scale] and [shape]. */
@Serializable
@SerialName("pareto")
public data class ParetoDistributionSpec(
    /** The scale parameter. */
    public val scale: Double,
    /** The shape parameter. */
    public val shape: Double,
) : DistributionSpec

/** A uniform distribution over the range from [lower] to [upper]. */
@Serializable
@SerialName("uniform")
public data class UniformDistributionSpec(
    /** The inclusive upper bound. */
    public val upper: Double,
    /** The inclusive lower bound. */
    public val lower: Double,
) : DistributionSpec {
    override fun validate(): List<ValidationIssue> = if (upper > lower) emptyList() else listOf(ValidationIssue("upper", "must be > lower"))
}

/** A Weibull distribution parameterized by [alpha] and [beta]. */
@Serializable
@SerialName("weibull")
public data class WeibullDistributionSpec(
    /** The scale parameter. */
    public val alpha: Double,
    /** The shape parameter. */
    public val beta: Double,
) : DistributionSpec {
    override fun validate(): List<ValidationIssue> =
        if (alpha > 0.0 && beta > 0.0) emptyList() else listOf(ValidationIssue("alpha", "alpha and beta must be > 0"))
}
