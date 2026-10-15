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
 * Represents energy values.
 * @see[Unit]
 */
@JvmInline
@Serializable(with = Energy.Companion.EnergySerializer::class)
public value class Energy private constructor(
    // In Joule
    override val value: Double,
) : Unit<Energy> {
    override fun toString(): String = fmtValue()

    /**
     * Joules below a kilojoule, kilojoules below a kilowatt-hour, and watt-hours from there on, as energy use is
     * usually reported in kWh and MWh rather than MJ and GJ.
     */
    override fun fmtValue(fmt: String): String =
        when (abs()) {
            in zero..<ofKJoule(1) -> "${toJoule().fmt(fmt)} J"
            in ofKJoule(1)..<ofKWh(1) -> "${toKJoule().fmt(fmt)} kJ"
            in ofKWh(1)..<ofMWh(1) -> "${toKWh().fmt(fmt)} kWh"
            else -> "${toMWh().fmt(fmt)} MWh"
        }

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Conversions to Double
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public fun toJoule(): Double = value

    public fun toKJoule(): Double = value / 1000

    public fun toMJoule(): Double = toKJoule() / 1000

    public fun toGJoule(): Double = toMJoule() / 1000

    public fun toWh(): Double = value / 3600

    public fun toKWh(): Double = toWh() / 1000

    public fun toMWh(): Double = toKWh() / 1000

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Operation Override (to avoid boxing of value classes in byte code)
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public override fun ifNeg0ThenPos0(): Energy = Energy(value.ifNeg0thenPos0())

    public override operator fun plus(other: Energy): Energy = Energy(value + other.value)

    public override operator fun minus(other: Energy): Energy = Energy(value - other.value)

    public override operator fun div(scalar: Number): Energy = Energy(value / scalar.toDouble())

    public override operator fun div(other: Energy): Percentage = Percentage.ofRatioUnchecked(value / other.value)

    public override operator fun times(scalar: Number): Energy = Energy(value * scalar.toDouble())

    public override operator fun times(percentage: Percentage): Energy = Energy(value * percentage.value)

    public override operator fun unaryMinus(): Energy = Energy(-value)

    public override operator fun compareTo(other: Energy): Int = this.value.compareTo(other.value)

    public override fun isZero(): Boolean = value == .0

    public override fun approxZero(epsilon: Double): Boolean = value.approx(.0, epsilon = epsilon)

    public override fun approx(
        other: Energy,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this == other || this.value.approx(other.value, minEpsilon, epsilon)

    public override infix fun approx(other: Energy): Boolean = approx(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxLarger(
        other: Energy,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxLarger(other.value, minEpsilon, epsilon)

    public override infix fun approxLarger(other: Energy): Boolean = approxLarger(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxLargerOrEq(
        other: Energy,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxLargerOrEq(other.value, minEpsilon, epsilon)

    public override infix fun approxLargerOrEq(other: Energy): Boolean = approxLargerOrEq(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxSmaller(
        other: Energy,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxSmaller(other.value, minEpsilon, epsilon)

    public override infix fun approxSmaller(other: Energy): Boolean = approxSmaller(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxSmallerOrEq(
        other: Energy,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxSmallerOrEq(other.value, minEpsilon, epsilon)

    public override infix fun approxSmallerOrEq(other: Energy): Boolean = approxSmallerOrEq(other, minEpsilon = DFLT_MIN_EPS)

    public override infix fun max(other: Energy): Energy = if (this.value > other.value) this else other

    public override infix fun min(other: Energy): Energy = if (this.value < other.value) this else other

    public override fun abs(): Energy = Energy(kotlin.math.abs(value))

    public override fun roundToIfWithinEpsilon(
        to: Energy,
        epsilon: Double,
    ): Energy =
        if (this.value in (to.value - epsilon)..(to.value + epsilon)) {
            to
        } else {
            this
        }

    public fun max(
        a: Energy,
        b: Energy,
    ): Energy = if (a.value > b.value) a else b

    public fun min(
        a: Energy,
        b: Energy,
    ): Energy = if (a.value < b.value) a else b

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Unit Specific Operations
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public operator fun div(timeDelta: TimeDelta): Power = Power.ofWatts(toWh() / timeDelta.toHours())

    public operator fun div(duration: Duration): Power = this / duration.toTimeDelta()

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Companion
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public companion object : UnitId<Energy> {
        @JvmStatic override val zero: Energy = Energy(.0)

        @JvmStatic override val max: Energy = Energy(Double.MAX_VALUE)

        @JvmStatic override val min: Energy = Energy(Double.MIN_VALUE)

        public operator fun Number.times(unit: Energy): Energy = unit * this

        @JvmStatic
        @JvmName("ofJoule")
        public fun ofJoule(joule: Number): Energy = Energy(joule.toDouble())

        @JvmStatic
        @JvmName("ofKJoule")
        public fun ofKJoule(kJoule: Number): Energy = ofJoule(kJoule.toDouble() * 1000)

        @JvmStatic
        @JvmName("ofMJoule")
        public fun ofMJoule(mJoule: Number): Energy = ofKJoule(mJoule.toDouble() * 1000)

        @JvmStatic
        @JvmName("ofGJoule")
        public fun ofGJoule(gJoule: Number): Energy = ofMJoule(gJoule.toDouble() * 1000)

        @JvmStatic
        @JvmName("ofWh")
        public fun ofWh(wh: Number): Energy = ofJoule(wh.toDouble() * 3600)

        @JvmStatic
        @JvmName("ofKWh")
        public fun ofKWh(kWh: Number): Energy = ofWh(kWh.toDouble() * 1000.0)

        @JvmStatic
        @JvmName("ofMWh")
        public fun ofMWh(mWh: Number): Energy = ofWh(mWh.toDouble() * 1000.0 * 1000.0)

        private val JOULES = Regex("\\s*(?:j|(?:joule|Joule)(?:|s))")

        // A watt-hour is a product, so the hour follows the watt directly ("kWh") or after a hyphen ("watts-hour").
        // The per-spellings ("kW/h") are still accepted, as they were the only ones that parsed before.
        private val WATT_HOUR = Regex("\\s*(?:-|p|per|/)?\\s*?")

        // //////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Serializer
        // //////////////////////////////////////////////////////////////////////////////////////////////////////////////

        /**
         * Serializer for [Energy] value class. It needs to be a compile
         * time constant to be used as serializer automatically,
         * hence `object :` instead of class instantiation.
         *
         * ```json
         * // e.g.
         * "energy": "1 KWh"
         * "energy": "  3    watts-hour  "
         * "energy": "2 megawatt-hours"
         * "energy": "10.5 Joules"
         * "energy": "2 GJ"
         * "energy": "1 mWh" // error: a lower-case "m" means milli, write "1 MWh" for a megawatt-hour
         * // etc.
         * ```
         */
        internal object EnergySerializer : UnitSerializer<Energy>(
            ifNumber = {
                LOG.warn(
                    "deserialization of number with no unit of measure, assuming it is in Joule" +
                        "Keep in mind that you can also specify the value as '$it Joule'",
                )
                ofJoule(it.toDouble())
            },
            serializerFun = { this.encodeString("${it.toJoule().toExactString()} J") },
            ifMatches("$NUM_GROUP$WATTS$WATT_HOUR$HOUR(?:|s)", IGNORE_CASE) { ofWh(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$KILO$WATTS$WATT_HOUR$HOUR(?:|s)", IGNORE_CASE) { ofKWh(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$UPPER_MEGA$WATTS$WATT_HOUR$HOUR(?:|s)", IGNORE_CASE) { ofMWh(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$JOULES", IGNORE_CASE) { ofJoule(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$KILO$JOULES", IGNORE_CASE) { ofKJoule(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$UPPER_MEGA$JOULES", IGNORE_CASE) { ofMJoule(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$GIGA$JOULES", IGNORE_CASE) { ofGJoule(json.decNumFromStr(groupValues[1])) },
            rejectMilli("$WATTS$WATT_HOUR$HOUR(?:|s)", example = "1 MWh"),
            rejectMilli("$JOULES", example = "1 MJ"),
        )
    }
}
