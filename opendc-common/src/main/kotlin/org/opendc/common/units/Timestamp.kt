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
import org.opendc.common.utils.ifNeg0thenPos0
import java.time.Duration
import java.time.Instant

/**
 * Represents timestamp values.
 * @see[Unit]
 */
@JvmInline
@Serializable(with = Timestamp.Companion.TimeStampSerializer::class)
public value class Timestamp private constructor(
    // In milliseconds since the Epoch.
    public override val value: Double,
) : Unit<Timestamp> {
    override fun toString(): String = fmtValue()

    /**
     * @return the [Instant] [toString] result of this [Timestamp] value.
     * @param[fmt] no ops.
     */
    override fun fmtValue(fmt: String): String = toInstant().toString()

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Conversions to Double
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public fun toEpochNs(): Double = value * 1e6

    public fun toEpochMicros(): Double = value * 1e3

    public fun toEpochMs(): Double = value

    public fun toEpochSec(): Double = value / 1000.0

    public fun toEpochMin(): Double = toEpochSec() / 60

    public fun toEpochHours(): Double = toEpochMin() / 60

    public fun toEpochDays(): Double = toEpochHours() / 24

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Operation Override (to avoid boxing of value classes in byte code)
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public override fun ifNeg0ThenPos0(): Timestamp = Timestamp(value.ifNeg0thenPos0())

    @UnintendedOperation
    public override operator fun plus(other: Timestamp): Timestamp = throw UnitOperationException()

    @UnintendedOperation
    public override operator fun minus(other: Timestamp): Timestamp = throw UnitOperationException()

    public override operator fun div(scalar: Number): Timestamp = Timestamp(value / scalar.toDouble())

    public override operator fun div(other: Timestamp): Percentage = Percentage.ofRatio(value / other.value)

    public override operator fun times(scalar: Number): Timestamp = Timestamp(value * scalar.toDouble())

    @UnintendedOperation
    public override operator fun times(percentage: Percentage): Timestamp = throw UnitOperationException()

    @UnintendedOperation
    public override operator fun unaryMinus(): Timestamp = throw UnitOperationException()

    public override operator fun compareTo(other: Timestamp): Int = this.value.compareTo(other.value)

    public override fun isZero(): Boolean = value == .0

    public override fun approxZero(epsilon: Double): Boolean = value.approx(.0, epsilon = epsilon)

    public override fun approx(
        other: Timestamp,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this == other || this.value.approx(other.value, minEpsilon, epsilon)

    public override infix fun approx(other: Timestamp): Boolean = approx(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxLarger(
        other: Timestamp,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxLarger(other.value, minEpsilon, epsilon)

    public override infix fun approxLarger(other: Timestamp): Boolean = approxLarger(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxLargerOrEq(
        other: Timestamp,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxLargerOrEq(other.value, minEpsilon, epsilon)

    public override infix fun approxLargerOrEq(other: Timestamp): Boolean = approxLargerOrEq(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxSmaller(
        other: Timestamp,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxSmaller(other.value, minEpsilon, epsilon)

    public override infix fun approxSmaller(other: Timestamp): Boolean = approxSmaller(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxSmallerOrEq(
        other: Timestamp,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxSmallerOrEq(other.value, minEpsilon, epsilon)

    public override infix fun approxSmallerOrEq(other: Timestamp): Boolean = approxSmallerOrEq(other, minEpsilon = DFLT_MIN_EPS)

    public override infix fun max(other: Timestamp): Timestamp = if (this.value > other.value) this else other

    public override infix fun min(other: Timestamp): Timestamp = if (this.value < other.value) this else other

    public override fun abs(): Timestamp = Timestamp(kotlin.math.abs(value))

    public override fun roundToIfWithinEpsilon(
        to: Timestamp,
        epsilon: Double,
    ): Timestamp =
        if (this.value in (to.value - epsilon)..(to.value + epsilon)) {
            to
        } else {
            this
        }

    public fun max(
        a: Timestamp,
        b: Timestamp,
    ): Timestamp = if (a.value > b.value) a else b

    public fun min(
        a: Timestamp,
        b: Timestamp,
    ): Timestamp = if (a.value < b.value) a else b

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Unit Specific Operations
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public operator fun minus(timeDelta: TimeDelta): Timestamp = Timestamp(value - timeDelta.value)

    public operator fun plus(timeDelta: TimeDelta): Timestamp = Timestamp(value + timeDelta.value)

    /**
     * @return the [TimeDelta] between *this* and [other]. Be aware that this is not the absolute value, it can be negative.
     */
    public infix fun timeDelta(other: Timestamp): TimeDelta = this.toTimeDeltaFromEpoch() - other.toTimeDeltaFromEpoch()

    public fun toInstant(): Instant =
        if (toEpochMs() > Long.MAX_VALUE) {
            Instant.ofEpochSecond(toEpochSec().toLong())
        } else {
            Instant.ofEpochMilli(toEpochMs().toLong())
        }

    public fun toTimeDeltaFromEpoch(): TimeDelta = TimeDelta.ofMillis(toEpochMs())

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Companion
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public companion object : UnitId<Timestamp> {
        @JvmStatic override val zero: Timestamp = Timestamp(.0)

        @JvmStatic override val max: Timestamp = Timestamp(Double.MAX_VALUE)

        @JvmStatic override val min: Timestamp = Timestamp(Double.MIN_VALUE)

        public operator fun Number.times(unit: Timestamp): Timestamp = unit * this

        @JvmStatic
        @JvmName("ofEpochNs")
        public fun ofEpochNs(nanos: Number): Timestamp = Timestamp(nanos.toDouble() / 1e6)

        @JvmStatic
        @JvmName("ofEpochMicros")
        public fun ofEpochMicros(micros: Number): Timestamp = Timestamp(micros.toDouble() / 1e3)

        @JvmStatic
        @JvmName("ofEpochMs")
        public fun ofEpochMs(ms: Number): Timestamp = Timestamp(ms.toDouble())

        @JvmStatic
        @JvmName("ofEpochSec")
        public fun ofEpochSec(sec: Number): Timestamp = ofEpochMs(sec.toDouble() * 1000.0)

        @JvmStatic
        @JvmName("ofEpochMin")
        public fun ofEpochMin(sec: Number): Timestamp = ofEpochSec(sec.toDouble() * 60)

        @JvmStatic
        @JvmName("ofEpochHours")
        public fun ofEpochHours(sec: Number): Timestamp = ofEpochMin(sec.toDouble() * 60)

        @JvmStatic
        @JvmName("ofInstant")
        public fun ofInstant(instant: Instant): Timestamp = ofEpochMs(instant.toEpochMilli())

        @JvmStatic
        @JvmName("toTimestamp")
        public fun Instant.toTimestamp(): Timestamp = ofEpochMs(toEpochMilli())

        // //////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Serializer
        // //////////////////////////////////////////////////////////////////////////////////////////////////////////////

        /**
         * Serializer for [Timestamp] value class. It needs to be a compile
         * time constant to be used as serializer automatically,
         * hence `object :` instead of class instantiation.
         *
         * ```json
         * // e.g.
         * "timestamp": "10000" // 10,000 ms since Epoch
         * "timestamp": "2001-09-09T01:48:19Z"
         * // etc.
         * ```
         */
        internal object TimeStampSerializer : UnitSerializer<Timestamp>(
            ifNumber = {
                LOG.warn(
                    "deserialization of number with no unit of measure, assuming it is in milliseconds since Epoch." +
                        "Keep in mind that you can also specify the value with timestamp representation (e.g. '2001-09-09T01:48:19Z')",
                )
                ofEpochMs(it.toDouble())
            },
            serializerFun = { this.encodeString(it.toString()) },
            ifNoExc { ofInstant(Instant.parse(this)) },
            ifNoExc {
                val duration = Duration.parse(this)
                LOG.warn("timestamp value was expected but duration string representation found. Assuming it is a duration since Epoch.")

                ofEpochMs(duration.toTimeDelta().toMs())
            },
        )
    }
}
