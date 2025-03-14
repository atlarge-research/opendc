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
import org.opendc.common.units.TimeDelta.Companion.toTimeDelta
import org.opendc.common.utils.DFLT_MIN_EPS
import org.opendc.common.utils.approx
import org.opendc.common.utils.approxLarger
import org.opendc.common.utils.approxLargerOrEq
import org.opendc.common.utils.approxSmaller
import org.opendc.common.utils.approxSmallerOrEq
import org.opendc.common.utils.fmt
import org.opendc.common.utils.ifNeg0thenPos0
import java.time.Duration
import kotlin.text.RegexOption.IGNORE_CASE

/**
 * Represents power values.
 * @see[Unit]
 */
@JvmInline
@Serializable(with = Power.Companion.PowerSerializer::class)
public value class Power private constructor(
    // In Watts.
    override val value: Double,
) : Unit<Power> {
    override fun toString(): String = fmtValue()

    override fun fmtValue(fmt: String): String =
        if (value >= 1000.0) {
            "${toKWatts().fmt(fmt)} KWatts"
        } else {
            "${toWatts().fmt(fmt)} Watts"
        }

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Conversions to Double
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public fun toWatts(): Double = value

    public fun toKWatts(): Double = value / 1000.0

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Operation Override (to avoid boxing of value classes in byte code)
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public override fun ifNeg0ThenPos0(): Power = Power(value.ifNeg0thenPos0())

    public override operator fun plus(other: Power): Power = Power(value + other.value)

    public override operator fun minus(other: Power): Power = Power(value - other.value)

    public override operator fun div(scalar: Number): Power = Power(value / scalar.toDouble())

    public override operator fun div(other: Power): Percentage = Percentage.ofRatio(value / other.value)

    public override operator fun times(scalar: Number): Power = Power(value * scalar.toDouble())

    public override operator fun times(percentage: Percentage): Power = Power(value * percentage.value)

    public override operator fun unaryMinus(): Power = Power(-value)

    public override operator fun compareTo(other: Power): Int = this.value.compareTo(other.value)

    public override fun isZero(): Boolean = value == .0

    public override fun approxZero(epsilon: Double): Boolean = value.approx(.0, epsilon = epsilon)

    public override fun approx(
        other: Power,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this == other || this.value.approx(other.value, minEpsilon, epsilon)

    public override infix fun approx(other: Power): Boolean = approx(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxLarger(
        other: Power,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxLarger(other.value, minEpsilon, epsilon)

    public override infix fun approxLarger(other: Power): Boolean = approxLarger(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxLargerOrEq(
        other: Power,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxLargerOrEq(other.value, minEpsilon, epsilon)

    public override infix fun approxLargerOrEq(other: Power): Boolean = approxLargerOrEq(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxSmaller(
        other: Power,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxSmaller(other.value, minEpsilon, epsilon)

    public override infix fun approxSmaller(other: Power): Boolean = approxSmaller(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxSmallerOrEq(
        other: Power,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxSmallerOrEq(other.value, minEpsilon, epsilon)

    public override infix fun approxSmallerOrEq(other: Power): Boolean = approxSmallerOrEq(other, minEpsilon = DFLT_MIN_EPS)

    public override infix fun max(other: Power): Power = if (this.value > other.value) this else other

    public override infix fun min(other: Power): Power = if (this.value < other.value) this else other

    public override fun abs(): Power = Power(kotlin.math.abs(value))

    public override fun roundToIfWithinEpsilon(
        to: Power,
        epsilon: Double,
    ): Power =
        if (this.value in (to.value - epsilon)..(to.value + epsilon)) {
            to
        } else {
            this
        }

    public fun max(
        a: Power,
        b: Power,
    ): Power = if (a.value > b.value) a else b

    public fun min(
        a: Power,
        b: Power,
    ): Power = if (a.value < b.value) a else b

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Unit Specific Operations
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public operator fun times(timeDelta: TimeDelta): Energy = Energy.ofWh(toWatts() * timeDelta.toHours())

    public operator fun times(duration: Duration): Energy = this * duration.toTimeDelta()

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Companion
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public companion object : UnitId<Power> {
        @JvmStatic override val zero: Power = Power(.0)

        @JvmStatic override val max: Power = Power(Double.MAX_VALUE)

        @JvmStatic override val min: Power = Power(Double.MIN_VALUE)

        public operator fun Number.times(unit: Power): Power = unit * this

        @JvmStatic
        @JvmName("ofWatts")
        public fun ofWatts(watts: Number): Power = Power(watts.toDouble())

        @JvmStatic
        @JvmName("ofKWatts")
        public fun ofKWatts(kWatts: Number): Power = Power(kWatts.toDouble() * 1000.0)

        // //////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Serializer
        // //////////////////////////////////////////////////////////////////////////////////////////////////////////////

        /**
         * Serializer for [Power] value class. It needs to be a compile
         * time constant to be used as serializer automatically,
         * hence `object :` instead of class instantiation.
         *
         * ```json
         * // e.g.
         * "power-draw": "4 watts"
         * "power-draw": "  1    KWatt   "
         * // etc.
         * ```
         */
        internal object PowerSerializer : UnitSerializer<Power>(
            ifNumber = {
                LOG.warn(
                    "deserialization of number with no unit of measure, assuming it is in Watts." +
                        "Keep in mind that you can also specify the value as '$it W'",
                )
                ofWatts(it.toDouble())
            },
            serializerFun = { this.encodeString(it.toString()) },
            ifMatches("$NUM_GROUP$WATTS", IGNORE_CASE) { ofWatts(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$KILO$WATTS", IGNORE_CASE) { ofKWatts(json.decNumFromStr(groupValues[1])) },
        )
    }
}
