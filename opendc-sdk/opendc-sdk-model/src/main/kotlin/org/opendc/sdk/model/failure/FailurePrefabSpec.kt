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

import kotlinx.serialization.Serializable

/**
 * A named, built-in failure model derived from an empirical availability trace fitted to a
 * statistical distribution. The prefix identifies the source dataset and the suffix the fitted
 * distribution family (Exp, Wbl, LogN, Gam).
 *
 * The values are taken from "The Failure Trace Archive: Enabling the comparison of failure
 * measurements and models of distributed systems" (https://www.sciencedirect.com/science/article/pii/S0743731513000634).
 */
@Serializable
public enum class FailurePrefabSpec(
    /** Distribution of the time between successive failures, in hours. */
    public val interArrival: DistributionSpec,
    /** Distribution of the duration of each failure, in hours. */
    public val duration: DistributionSpec,
) {
    G5k06Exp(
        ExponentialDistributionSpec(32.41),
        ExponentialDistributionSpec(7.41),
    ),
    G5k06Wbl(
        WeibullDistributionSpec(0.48, 14.37),
        WeibullDistributionSpec(0.35, 0.47),
    ),
    G5k06LogN(
        LogNormalDistributionSpec(1.51, 2.42),
        LogNormalDistributionSpec(-2.0, 2.2),
    ),
    G5k06Gam(
        GammaDistributionSpec(0.34, 94.35),
        GammaDistributionSpec(0.19, 39.92),
    ),
    Lanl05Exp(
        ExponentialDistributionSpec(1779.99),
        ExponentialDistributionSpec(5.92),
    ),
    Lanl05Wbl(
        WeibullDistributionSpec(0.48, 816.60),
        WeibullDistributionSpec(0.58, 2.18),
    ),
    Lanl05LogN(
        LogNormalDistributionSpec(5.56, 2.39),
        LogNormalDistributionSpec(0.05, 1.42),
    ),
    Lanl05Gam(
        GammaDistributionSpec(0.35, 5102.71),
        GammaDistributionSpec(0.38, 15.44),
    ),
    Ldns04Exp(
        ExponentialDistributionSpec(141.06),
        ExponentialDistributionSpec(8.61),
    ),
    Ldns04Wbl(
        WeibullDistributionSpec(0.51, 79.30),
        WeibullDistributionSpec(0.63, 5.62),
    ),
    Ldns04LogN(
        LogNormalDistributionSpec(3.25, 2.33),
        LogNormalDistributionSpec(0.91, 1.64),
    ),
    Ldns04Gam(
        GammaDistributionSpec(0.39, 362.43),
        GammaDistributionSpec(0.51, 16.87),
    ),
    Microsoft99Exp(
        ExponentialDistributionSpec(67.01),
        ExponentialDistributionSpec(16.49),
    ),
    Microsoft99Wbl(
        WeibullDistributionSpec(0.55, 35.30),
        WeibullDistributionSpec(0.60, 9.34),
    ),
    Microsoft99LogN(
        LogNormalDistributionSpec(2.62, 1.84),
        LogNormalDistributionSpec(1.42, 1.54),
    ),
    Microsoft99Gam(
        GammaDistributionSpec(0.41, 162.19),
        GammaDistributionSpec(0.46, 35.52),
    ),
    Nd07cpuExp(
        ExponentialDistributionSpec(13.73),
        ExponentialDistributionSpec(4.25),
    ),
    Nd07cpuWbl(
        WeibullDistributionSpec(0.45, 4.16),
        WeibullDistributionSpec(0.51, 0.74),
    ),
    Nd07cpuLogN(
        LogNormalDistributionSpec(0.30, 2.20),
        LogNormalDistributionSpec(-1.02, 1.27),
    ),
    Nd07cpuGam(
        GammaDistributionSpec(0.30, 46.16),
        GammaDistributionSpec(0.28, 15.07),
    ),
    Overnet03Exp(
        ExponentialDistributionSpec(2.29),
        ExponentialDistributionSpec(12.00),
    ),
    Overnet03Wbl(
        WeibullDistributionSpec(0.85, 2.04),
        WeibullDistributionSpec(0.44, 2.98),
    ),
    Overnet03LogN(
        LogNormalDistributionSpec(0.19, 0.98),
        LogNormalDistributionSpec(0.08, 1.80),
    ),
    Overnet03Gam(
        GammaDistributionSpec(0.91, 2.53),
        GammaDistributionSpec(0.29, 41.64),
    ),
    Pl05Exp(
        ExponentialDistributionSpec(159.49),
        ExponentialDistributionSpec(49.61),
    ),
    Pl05Wbl(
        WeibullDistributionSpec(0.33, 19.35),
        WeibullDistributionSpec(0.36, 5.59),
    ),
    Pl05LogN(
        LogNormalDistributionSpec(1.44, 2.86),
        LogNormalDistributionSpec(0.40, 2.45),
    ),
    Pl05Gam(
        GammaDistributionSpec(0.20, 788.03),
        GammaDistributionSpec(0.21, 237.65),
    ),
    Skype06Exp(
        ExponentialDistributionSpec(16.27),
        ExponentialDistributionSpec(14.31),
    ),
    Skype06Wbl(
        WeibullDistributionSpec(0.64, 10.86),
        WeibullDistributionSpec(0.63, 9.48),
    ),
    Skype06LogN(
        LogNormalDistributionSpec(1.60, 1.57),
        LogNormalDistributionSpec(1.40, 1.73),
    ),
    Skype06Gam(
        GammaDistributionSpec(0.53, 30.79),
        GammaDistributionSpec(0.50, 28.53),
    ),
    Websites02Exp(
        ExponentialDistributionSpec(11.85),
        ExponentialDistributionSpec(1.18),
    ),
    Websites02Wbl(
        WeibullDistributionSpec(0.46, 3.68),
        WeibullDistributionSpec(0.65, 0.61),
    ),
    Websites02LogN(
        LogNormalDistributionSpec(0.23, 2.02),
        LogNormalDistributionSpec(-1.12, 1.13),
    ),
    Websites02Gam(
        GammaDistributionSpec(0.31, 38.67),
        GammaDistributionSpec(0.50, 2.37),
    ),
    ;

    /** The fully specified failure model this prefab stands for. Every prefab fails a uniformly sampled fraction of hosts. */
    public fun toCustomSpec(): CustomFailureSpec =
        CustomFailureSpec(interArrival, duration, hostFraction = UniformDistributionSpec(lower = 0.0, upper = 1.0))
}
