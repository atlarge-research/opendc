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

package org.opendc.common.utils

import kotlin.math.abs

/**
 * When comparing 2 doubles, `==` can produce wrong results. The threshold comparison method check that
 * the difference between both numbers is within a specified tolerance, commonly called epsilon.
 * In this case we use adaptive epsilons, meaning the epsilon is adjusted proportionally to
 * the values that are being compared.
 *
 * This value represents the default epsilon multiplier used if an epsilon is not provided.
 */
internal const val DFLT_EPS_MULTIPLIER: Double = 1e-05
internal const val DFLT_MIN_EPS: Double = 1.0e-06

/**
 * Compares [this] with [other] using threshold comparison method with epsilon = [epsilon].
 *
 * @param[minEpsilon] the minimum epsilon that can be computed when [epsilon] is not provided.
 * If [epsilon] is provided, this param has no effect.
 * @param[epsilon] represent the tolerance of the comparison.
 * If not provided an adaptive epsilon is computed (based on the largest value in the comparison).
 * @return `true` if [this] is considered equal to [other], `false` otherwise.
 */
@JvmOverloads
public fun Double.approx(
    other: Double,
    minEpsilon: Double = DFLT_MIN_EPS,
    epsilon: Double = adaptiveEps(this, other, minEpsilon),
): Boolean = this == other || abs(this - other) <= epsilon

/**
 * Infix version of [approx].
 * @see[approx]
 */
@JvmSynthetic
@JvmName("approx, jvm name to avoid same jvm signature (not invokable from java)")
public infix fun Double.approx(other: Double): Boolean = approx(other, epsilon = DFLT_EPS_MULTIPLIER)

/**
 * @return [this] approximated to [to] if within `0 - epsilon` and `0 + epsilon`.
 */
@JvmOverloads
public fun Double.roundToIfWithinEpsilon(
    to: Double,
    epsilon: Double = DFLT_MIN_EPS,
): Double =
    if (this in (to - epsilon)..(to + epsilon)) {
        to
    } else {
        this
    }

/**
 * Compares [this] with [other] using threshold comparison method with epsilon = [epsilon].
 *
 * @param[minEpsilon] the minimum epsilon that can be computed when [epsilon] is not provided.
 * If [epsilon] is provided, this param has no effect.
 * @param[epsilon] represent the tolerance of the comparison.
 * If not provided an adaptive epsilon is computed (based on the largest value in the comparison).
 * @return `true` if [this] is considered larger than [other], `false` otherwise.
 */
@JvmOverloads
public fun Double.approxLarger(
    other: Double,
    minEpsilon: Double = DFLT_MIN_EPS,
    epsilon: Double = adaptiveEps(this, other, minEpsilon),
): Boolean = (this - other) > epsilon

/**
 * Infix version of [approxLarger].
 * @see[approxLarger]
 */
@JvmSynthetic
@JvmName("approxLarger, jvm name to avoid same jvm signature (not invokable from java)")
public infix fun Double.approxLarger(other: Double): Boolean = this.approxLarger(other, epsilon = DFLT_EPS_MULTIPLIER)

/**
 * Compares [this] with [other] using threshold comparison method with epsilon = [epsilon].
 *
 * @param[minEpsilon] the minimum epsilon that can be computed when [epsilon] is not provided.
 * If [epsilon] is provided, this param has no effect.
 * @param[epsilon] represent the tolerance of the comparison.
 * If not provided an adaptive epsilon is computed (based on the largest value in the comparison).
 * @return `true` if [this] is considered larger or equal than [other], `false` otherwise.
 */
@JvmOverloads
public fun Double.approxLargerOrEq(
    other: Double,
    minEpsilon: Double = DFLT_MIN_EPS,
    epsilon: Double = adaptiveEps(this, other, minEpsilon),
): Boolean = (this - other) > -epsilon

/**
 * Infix version of [approxLargerOrEq].
 * @see[approxLargerOrEq]
 */
@JvmSynthetic
@JvmName("approxLargerOrEq, jvm name to avoid same jvm signature (not invokable from java)")
public infix fun Double.approxLargerOrEq(other: Double): Boolean = this.approxLargerOrEq(other, epsilon = DFLT_EPS_MULTIPLIER)

/**
 * Compares [this] with [other] using threshold comparison method with epsilon = [epsilon].
 *
 * @param[minEpsilon] the minimum epsilon that can be computed when [epsilon] is not provided.
 * If [epsilon] is provided, this param has no effect.
 * @param[epsilon] represent the tolerance of the comparison.
 * If not provided an adaptive epsilon is computed (based on the largest value in the comparison).
 * @return `true` if [this] is considered smaller than [other], `false` otherwise.
 */
@JvmOverloads
public fun Double.approxSmaller(
    other: Double,
    minEpsilon: Double = DFLT_MIN_EPS,
    epsilon: Double = adaptiveEps(this, other, minEpsilon),
): Boolean = (this - other) < -epsilon

/**
 * Infix version of [approxLarger].
 * @see[approxLarger]
 */
@JvmSynthetic
@JvmName("approxSmaller, jvm name to avoid same jvm signature (not invokable from java)")
public infix fun Double.approxSmaller(other: Double): Boolean = this.approxLarger(other, epsilon = DFLT_EPS_MULTIPLIER)

/**
 * Compares [this] with [other] using threshold comparison method with epsilon = [epsilon].
 *
 * @param[minEpsilon] the minimum epsilon that can be computed when [epsilon] is not provided.
 * If [epsilon] is provided, this param has no effect.
 * @param[epsilon] represent the tolerance of the comparison.
 * If not provided an adaptive epsilon is computed (based on the largest value in the comparison).
 * @return `true` if [this] is considered smaller or equal than [other], `false` otherwise.
 */
@JvmOverloads
public fun Double.approxSmallerOrEq(
    other: Double,
    minEpsilon: Double = DFLT_MIN_EPS,
    epsilon: Double = adaptiveEps(this, other, minEpsilon),
): Boolean = this - other < epsilon

/**
 * Infix version of [approxSmallerOrEq].
 * @see[approxSmallerOrEq]
 */
@JvmSynthetic
@JvmName("approxSmallerOrEq, jvm name to avoid same jvm signature (not invokable from java)")
public infix fun Double.approxSmallerOrEq(other: Double): Boolean = approxSmallerOrEq(other, DFLT_EPS_MULTIPLIER)

/**
 * @return the result of [block] if [this] is NaN, [this] otherwise.
 */
public inline infix fun Double.ifNaN(block: () -> Double): Double =
    if (this.isNaN()) {
        block()
    } else {
        this
    }

/**
 * @return [replacement] if [this] is NaN, [this] otherwise.
 */
public infix fun Double.ifNaN(replacement: Double): Double =
    if (this.isNaN()) {
        replacement
    } else {
        this
    }

/**
 * @return adaptive epsilon computed proportionally to the max absolute value of [a] and [b]
 */
internal fun adaptiveEps(
    a: Double,
    b: Double,
    minEpsilon: Double = DFLT_MIN_EPS,
): Double = DFLT_EPS_MULTIPLIER * maxOf(minEpsilon, abs(a), abs(b))

/**
 * ```kotlin
 * // replace
 * String.format("%.3f", doubleValue)
 * // with
 * doubleValue.fmt("%.3f")
 * ```
 *
 * @return [this] formatted by [fmt].
 */
public fun Double.fmt(fmt: String): String = String.format(fmt, this)
