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
 * Represents frequency values.
 * @see[Unit]
 */
@JvmInline
@Serializable(with = Frequency.Companion.FrequencySerializer::class)
public value class Frequency private constructor(
    // As MHz.
    override val value: Double,
) : Unit<Frequency> {
    override fun toString(): String = fmtValue()

    override fun fmtValue(fmt: String): String =
        when (abs()) {
            in zero..ofHz(500) -> "${toHz().fmt(fmt)} Hz"
            in ofHz(500)..ofKHz(500) -> "${toKHz().fmt(fmt)} KHz"
            in ofKHz(500)..ofMHz(500) -> "${toMHz().fmt(fmt)} MHz"
            else -> "${toGHz().fmt(fmt)} GHz"
        }

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Conversions to Double
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public fun toHz(): Double = value * 1e6

    public fun toKHz(): Double = value * 1e3

    public fun toMHz(): Double = value

    public fun toGHz(): Double = value / 1e3

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Operation Override (to avoid boxing of value classes in byte code)
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public override fun ifNeg0ThenPos0(): Frequency = Frequency(value.ifNeg0thenPos0())

    public override operator fun plus(other: Frequency): Frequency = Frequency(value + other.value)

    public override operator fun minus(other: Frequency): Frequency = Frequency(value - other.value)

    public override operator fun div(scalar: Number): Frequency = Frequency(value / scalar.toDouble())

    public override operator fun div(other: Frequency): Percentage = Percentage.ofRatio(value / other.value)

    public override operator fun times(scalar: Number): Frequency = Frequency(value * scalar.toDouble())

    public override operator fun times(percentage: Percentage): Frequency = Frequency(value * percentage.value)

    public override operator fun unaryMinus(): Frequency = Frequency(-value)

    public override operator fun compareTo(other: Frequency): Int = this.value.compareTo(other.value)

    public override fun isZero(): Boolean = value == .0

    public override fun approxZero(epsilon: Double): Boolean = value.approx(.0, epsilon = epsilon)

    public override fun approx(
        other: Frequency,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this == other || this.value.approx(other.value, minEpsilon, epsilon)

    public override infix fun approx(other: Frequency): Boolean = approx(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxLarger(
        other: Frequency,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxLarger(other.value, minEpsilon, epsilon)

    public override infix fun approxLarger(other: Frequency): Boolean = approxLarger(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxLargerOrEq(
        other: Frequency,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxLargerOrEq(other.value, minEpsilon, epsilon)

    public override infix fun approxLargerOrEq(other: Frequency): Boolean = approxLargerOrEq(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxSmaller(
        other: Frequency,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxSmaller(other.value, minEpsilon, epsilon)

    public override infix fun approxSmaller(other: Frequency): Boolean = approxSmaller(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxSmallerOrEq(
        other: Frequency,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxSmallerOrEq(other.value, minEpsilon, epsilon)

    public override infix fun approxSmallerOrEq(other: Frequency): Boolean = approxSmallerOrEq(other, minEpsilon = DFLT_MIN_EPS)

    public override infix fun max(other: Frequency): Frequency = if (this.value > other.value) this else other

    public override infix fun min(other: Frequency): Frequency = if (this.value < other.value) this else other

    public override fun abs(): Frequency = Frequency(kotlin.math.abs(value))

    public override fun roundToIfWithinEpsilon(
        to: Frequency,
        epsilon: Double,
    ): Frequency =
        if (this.value in (to.value - epsilon)..(to.value + epsilon)) {
            to
        } else {
            this
        }

    public fun max(
        a: Frequency,
        b: Frequency,
    ): Frequency = if (a.value > b.value) a else b

    public fun min(
        a: Frequency,
        b: Frequency,
    ): Frequency = if (a.value < b.value) a else b

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Unit Specific Operations
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public operator fun times(timeDelta: TimeDelta): Double = toHz() * timeDelta.toSec()

    public operator fun times(duration: Duration): Double = toHz() * duration.toTimeDelta().toSec()

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Companion
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public companion object : UnitId<Frequency> {
        @JvmStatic override val zero: Frequency = Frequency(.0)

        @JvmStatic override val max: Frequency = Frequency(Double.MAX_VALUE)

        @JvmStatic override val min: Frequency = Frequency(Double.MIN_VALUE)

        public operator fun Number.times(unit: Frequency): Frequency = unit * this

        @JvmStatic
        @JvmName("ofHz")
        public fun ofHz(hz: Number): Frequency = ofMHz(hz.toDouble() / 1e6)

        @JvmStatic
        @JvmName("ofKHz")
        public fun ofKHz(kHz: Number): Frequency = ofMHz(kHz.toDouble() / 1e3)

        @JvmStatic
        @JvmName("ofMHz")
        public fun ofMHz(mHz: Number): Frequency = Frequency(mHz.toDouble())

        @JvmStatic
        @JvmName("ofGHz")
        public fun ofGHz(gHz: Number): Frequency = ofMHz(gHz.toDouble() * 1e3)

        private val HERTZ = Regex("\\s*(?:Hz|Hertz|hz|hertz)\\s*?")

        // //////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Serializer
        // //////////////////////////////////////////////////////////////////////////////////////////////////////////////

        /**
         * Serializer for [Frequency] value class. It needs to be a compile
         * time constant to be used as serializer automatically,
         * hence `object :` instead of class instantiation.
         *
         * ```json
         * // e.g.
         * "frequency": "1000 Hz"
         * "frequency": "  10    GHz   "
         * "frequency": "2megahertz"
         * // etc.
         * ```
         */
        internal object FrequencySerializer : UnitSerializer<Frequency>(
            ifNumber = {
                LOG.warn("deserialization of number with no unit of measure, assuming it is in MHz...")
                ofMHz(it.toDouble())
            },
            serializerFun = { this.encodeString(it.toString()) },
            ifMatches("$NUM_GROUP$HERTZ", IGNORE_CASE) { ofHz(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$KILO$HERTZ", IGNORE_CASE) { ofKHz(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$MEGA$HERTZ", IGNORE_CASE) { ofMHz(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$GIGA$HERTZ", IGNORE_CASE) { ofGHz(json.decNumFromStr(groupValues[1])) },
        )
    }
}
