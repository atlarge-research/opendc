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
import org.opendc.common.utils.ifNeg0thenPos0
import java.time.Duration
import java.time.Instant
import kotlin.text.RegexOption.IGNORE_CASE

/**
 * Represents time interval values.
 * @see[Unit]
 */
@JvmInline
@Serializable(with = TimeDelta.Companion.TimeDeltaSerializer::class)
public value class TimeDelta private constructor(
    // In milliseconds.
    public override val value: Double,
) : Unit<TimeDelta> {
    override fun toString(): String = fmtValue()

    override fun fmtValue(fmt: String): String = Duration.ofMillis(value.toLong()).toString()

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Conversions to Double
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public fun toNs(): Double = value * 1e6

    public fun toMicros(): Double = value * 1e3

    public fun toMs(): Double = value

    public fun toMsLong(): Long = value.toLong()

    public fun toSec(): Double = value / 1000.0

    public fun toMin(): Double = toSec() / 60

    public fun toHours(): Double = toMin() / 60

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Operation Override (to avoid boxing of value classes in byte code)
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public override fun ifNeg0ThenPos0(): TimeDelta = TimeDelta(value.ifNeg0thenPos0())

    public override operator fun plus(other: TimeDelta): TimeDelta = TimeDelta(value + other.value)

    public override operator fun minus(other: TimeDelta): TimeDelta = TimeDelta(value - other.value)

    public override operator fun div(scalar: Number): TimeDelta = TimeDelta(value / scalar.toDouble())

    public override operator fun div(other: TimeDelta): Percentage = Percentage.ofRatio(value / other.value)

    public override operator fun times(scalar: Number): TimeDelta = TimeDelta(value * scalar.toDouble())

    public override operator fun times(percentage: Percentage): TimeDelta = TimeDelta(value * percentage.value)

    public override operator fun unaryMinus(): TimeDelta = TimeDelta(-value)

    public override operator fun compareTo(other: TimeDelta): Int = this.value.compareTo(other.value)

    public override fun isZero(): Boolean = value == .0

    public override fun approxZero(epsilon: Double): Boolean = value.approx(.0, epsilon = epsilon)

    public override fun approx(
        other: TimeDelta,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this == other || this.value.approx(other.value, minEpsilon, epsilon)

    public override infix fun approx(other: TimeDelta): Boolean = approx(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxLarger(
        other: TimeDelta,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxLarger(other.value, minEpsilon, epsilon)

    public override infix fun approxLarger(other: TimeDelta): Boolean = approxLarger(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxLargerOrEq(
        other: TimeDelta,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxLargerOrEq(other.value, minEpsilon, epsilon)

    public override infix fun approxLargerOrEq(other: TimeDelta): Boolean = approxLargerOrEq(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxSmaller(
        other: TimeDelta,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxSmaller(other.value, minEpsilon, epsilon)

    public override infix fun approxSmaller(other: TimeDelta): Boolean = approxSmaller(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxSmallerOrEq(
        other: TimeDelta,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxSmallerOrEq(other.value, minEpsilon, epsilon)

    public override infix fun approxSmallerOrEq(other: TimeDelta): Boolean = approxSmallerOrEq(other, minEpsilon = DFLT_MIN_EPS)

    public override infix fun max(other: TimeDelta): TimeDelta = if (this.value > other.value) this else other

    public override infix fun min(other: TimeDelta): TimeDelta = if (this.value < other.value) this else other

    public override fun abs(): TimeDelta = TimeDelta(kotlin.math.abs(value))

    public override fun roundToIfWithinEpsilon(
        to: TimeDelta,
        epsilon: Double,
    ): TimeDelta =
        if (this.value in (to.value - epsilon)..(to.value + epsilon)) {
            to
        } else {
            this
        }

    public fun max(
        a: TimeDelta,
        b: TimeDelta,
    ): TimeDelta = if (a.value > b.value) a else b

    public fun min(
        a: TimeDelta,
        b: TimeDelta,
    ): TimeDelta = if (a.value < b.value) a else b

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Unit Specific Operations
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public fun toInstantFromEpoch(): Instant = Instant.ofEpochMilli(value.toLong())

    public operator fun times(power: Power): Energy = Energy.ofWh(toHours() * power.toWatts())

    public operator fun times(dataRate: DataRate): DataSize = DataSize.ofKB(toSec() * dataRate.toKBps())

    public companion object : UnitId<TimeDelta> {
        @JvmStatic override val zero: TimeDelta = TimeDelta(.0)

        @JvmStatic override val max: TimeDelta = TimeDelta(Double.MAX_VALUE)

        @JvmStatic override val min: TimeDelta = TimeDelta(Double.MIN_VALUE)

        public operator fun Number.times(unit: TimeDelta): TimeDelta = unit * this

        @JvmStatic
        @JvmName("ofNanos")
        public fun ofNanos(nanos: Number): TimeDelta = TimeDelta(nanos.toDouble() / 1e6)

        @JvmStatic
        @JvmName("ofMicros")
        public fun ofMicros(micros: Number): TimeDelta = TimeDelta(micros.toDouble() / 1e3)

        @JvmStatic
        @JvmName("ofMillis")
        public fun ofMillis(ms: Number): TimeDelta = TimeDelta(ms.toDouble())

        @JvmStatic
        @JvmName("ofSec")
        public fun ofSec(sec: Number): TimeDelta = TimeDelta(sec.toDouble() * 1000.0)

        @JvmStatic
        @JvmName("ofMin")
        public fun ofMin(min: Number): TimeDelta = TimeDelta(min.toDouble() * 60 * 1000.0)

        @JvmStatic
        @JvmName("ofHours")
        public fun ofHours(hours: Number): TimeDelta = TimeDelta(hours.toDouble() * 60 * 60 * 1000.0)

        @JvmStatic
        @JvmName("ofDuration")
        public fun ofDuration(duration: Duration): TimeDelta = duration.toTimeDelta()

        // //////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Serializer
        // //////////////////////////////////////////////////////////////////////////////////////////////////////////////

        /**
         * Serializer for [TimeDelta] value class. It needs to be a compile
         * time constant to be used as serializer automatically,
         * hence `object :` instead of class instantiation.
         *
         * ```json
         * // e.g.
         * "timedelta": "10 hours"
         * "timedelta": "  30    minutes   "
         * "timedelta": "1 ms"
         * "timedelta": "PT13H"
         * // etc.
         * ```
         */
        internal object TimeDeltaSerializer : UnitSerializer<TimeDelta>(
            ifNumber = {
                LOG.warn(
                    "deserialization of number with no unit of measure, assuming it is in milliseconds." +
                        "Keep in mind that you can also specify the value as '$it ms'",
                )
                ofMillis(it.toDouble())
            },
            serializerFun = { this.encodeString(it.toString()) },
            ifMatches("$NUM_GROUP$NANO$SEC(?:|s)\\s*", IGNORE_CASE) { ofNanos(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$MICRO$SEC(?:|s)\\s*", IGNORE_CASE) { ofMicros(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$MILLI$SEC(?:|s)\\s*", IGNORE_CASE) { ofMillis(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$SEC(?:|s)\\s*", IGNORE_CASE) { ofSec(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$MIN(?:|s)\\s*") { ofMin(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$HOUR(?:|s)\\s*") { ofHours(json.decNumFromStr(groupValues[1])) },
            ifNoExc { ofDuration(Duration.parse(this)) },
            ifNoExc {
                val instant = Instant.parse(this)
                LOG.warn("`TimeDelta` value was expected but `Instant` string representation found. Converting to `TimeDelta` since Epoch")

                ofMillis(instant.toEpochMilli())
            },
        )

        /**
         * @return [this] converted to a [TimeDelta] value, with the highest possible accuracy.
         *
         * @throws RuntimeException if [this] cannot be represented as nanos, millis, seconds, minutes or hours with a [Long].
         */
        public fun Duration.toTimeDelta(): TimeDelta {
            fun tryNoThrow(block: () -> TimeDelta?) =
                try {
                    block()
                } catch (_: Exception) {
                    null
                }

            return tryNoThrow { ofNanos(this.toNanos()) }
                ?: tryNoThrow { ofMillis(this.toMillis()) }
                ?: tryNoThrow { ofSec(this.toSeconds()) }
                ?: tryNoThrow { ofMin(this.toMinutes()) }
                ?: tryNoThrow { ofHours(this.toHours()) }
                ?: throw RuntimeException(
                    "duration $this cannot be converted to ${TimeDelta::class.simpleName}, " +
                        "duration value overflow Long representation of nanos, millis, seconds, minutes and hours",
                )
        }
    }
}
