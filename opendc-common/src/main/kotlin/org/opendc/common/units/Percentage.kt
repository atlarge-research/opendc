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

@file:OptIn(InternalUse::class, NonInlinableUnit::class)

package org.opendc.common.units

import kotlinx.serialization.Serializable
import org.opendc.common.annotations.InternalUse
import org.opendc.common.utils.DFLT_MIN_EPS
import org.opendc.common.utils.approx
import org.opendc.common.utils.approxLarger
import org.opendc.common.utils.approxLargerOrEq
import org.opendc.common.utils.approxSmaller
import org.opendc.common.utils.approxSmallerOrEq
import org.opendc.common.utils.fmt
import org.opendc.common.utils.ifNeg0thenPos0
import kotlin.text.RegexOption.IGNORE_CASE

/**
 * Represents a percentage.
 *
 * As all [Unit]s, offers the vast majority
 * of mathematical operations that one would perform on a simple [Double].
 */

@JvmInline
@Serializable(with = Percentage.Companion.PercentageSerializer::class)
public value class Percentage(
    override val value: Double,
) : Unit<Percentage> {
    override fun toString(): String = fmtValue()

    /**
     * ```kotlin
     * // e.g.
     * val perc: Percentage = Percentage.ofRatio(0.123456789)
     * perc.fmtValue("%.4f") // "12.3456%"
     * ```
     *
     * @see[Unit.fmtValue]
     */
    override fun fmtValue(fmt: String): String = "${toPercentageValue().fmt(fmt)}%"

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Conversions to Double
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * @return the value as a ratio (e.g., 50% -> 0.5)
     */
    public fun toRatio(): Double = value

    /**
     * @return the value as percentage (50.6% -> 50.6)
     */
    public fun toPercentageValue(): Double = value * 1e2

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Operation Override (to avoid boxing of value classes in byte code)
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public override fun ifNeg0ThenPos0(): Percentage = Percentage(value.ifNeg0thenPos0())

    public override operator fun plus(other: Percentage): Percentage = Percentage(value + other.value)

    public override operator fun minus(other: Percentage): Percentage = Percentage(value - other.value)

    public override operator fun div(scalar: Number): Percentage = Percentage(value / scalar.toDouble())

    public override operator fun div(other: Percentage): Percentage = Percentage.ofRatio(value / other.value)

    public override operator fun times(scalar: Number): Percentage = Percentage(value * scalar.toDouble())

    public override operator fun times(percentage: Percentage): Percentage = Percentage(value * percentage.value)

    public override operator fun unaryMinus(): Percentage = Percentage(-value)

    public override operator fun compareTo(other: Percentage): Int = this.value.compareTo(other.value)

    public override fun isZero(): Boolean = value == .0

    public override fun approxZero(epsilon: Double): Boolean = value.approx(.0, epsilon = epsilon)

    public override fun approx(
        other: Percentage,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this == other || this.value.approx(other.value, minEpsilon, epsilon)

    public override infix fun approx(other: Percentage): Boolean = approx(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxLarger(
        other: Percentage,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxLarger(other.value, minEpsilon, epsilon)

    public override infix fun approxLarger(other: Percentage): Boolean = approxLarger(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxLargerOrEq(
        other: Percentage,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxLargerOrEq(other.value, minEpsilon, epsilon)

    public override infix fun approxLargerOrEq(other: Percentage): Boolean = approxLargerOrEq(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxSmaller(
        other: Percentage,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxSmaller(other.value, minEpsilon, epsilon)

    public override infix fun approxSmaller(other: Percentage): Boolean = approxSmaller(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxSmallerOrEq(
        other: Percentage,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxSmallerOrEq(other.value, minEpsilon, epsilon)

    public override infix fun approxSmallerOrEq(other: Percentage): Boolean = approxSmallerOrEq(other, minEpsilon = DFLT_MIN_EPS)

    public override infix fun max(other: Percentage): Percentage = if (this.value > other.value) this else other

    public override infix fun min(other: Percentage): Percentage = if (this.value < other.value) this else other

    public override fun abs(): Percentage = Percentage(kotlin.math.abs(value))

    public override fun roundToIfWithinEpsilon(
        to: Percentage,
        epsilon: Double,
    ): Percentage =
        if (this.value in (to.value - epsilon)..(to.value + epsilon)) {
            to
        } else {
            this
        }

    public fun max(
        a: Percentage,
        b: Percentage,
    ): Percentage = if (a.value > b.value) a else b

    public fun min(
        a: Percentage,
        b: Percentage,
    ): Percentage = if (a.value < b.value) a else b

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Unit Specific Operations
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Companion
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public companion object : UnitId<Percentage> {
        @JvmStatic override val zero: Percentage = Percentage(.0)

        @JvmStatic override val max: Percentage = Percentage(Double.MAX_VALUE)

        @JvmStatic override val min: Percentage = Percentage(Double.MIN_VALUE)

        public operator fun Number.times(unit: Percentage): Percentage = unit * this

        @JvmStatic
        @JvmName("ofRatio")
        public fun ofRatio(ratio: Double): Percentage = Percentage(ratio)

        @JvmStatic
        @JvmName("ofPercentage")
        public fun ofPercentage(percentage: Number): Percentage = Percentage(percentage.toDouble() / 100)

        /**
         * @return the percentage resulting from [this] / [other].
         */
        public infix fun Number.percentageOf(other: Number): Percentage = Percentage(this.toDouble() / other.toDouble())

        /**
         * @return the percentage resulting from [this] / [other], applicable on all [Unit]s of the same type.
         */
        public infix fun <T : Unit<T>> T.percentageOf(other: T): Percentage = Percentage(this.value / other.value)

        // //////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Serializer
        // //////////////////////////////////////////////////////////////////////////////////////////////////////////////

        private val PERCENTAGE = Regex("\\s*(?:percentage|Percentage|%)\\s*?")

        /**
         * Serializer for [Percentage] value class. It needs to be a compile
         * time constant to be used as serializer automatically,
         * hence `object :` instead of class instantiation.
         *
         * For implementation purposes it always deserializes an [Percentage] as [Percentage].
         *
         * ```json
         * // e.g.
         * "percentage": 0.5 // 50% with warning
         * "percentage": "  30%   "
         * "percentage": "120%" // 120% (unbounded)
         * // etc.
         * ```
         */
        internal object PercentageSerializer : UnitSerializer<Percentage>(
            ifNumber = {
                LOG.warn(
                    "deserialization of number with no unit of measure, assuming it is a ratio." +
                        "Keep in mind that you can also specify the value as '${it.toDouble() * 100}%'",
                )
                ofRatio(it.toDouble())
            },
            serializerFun = { this.encodeString(it.toString()) },
            ifMatches("$NUM_GROUP$PERCENTAGE", IGNORE_CASE) { ofPercentage(json.decNumFromStr(groupValues[1])) },
        )
    }
}
