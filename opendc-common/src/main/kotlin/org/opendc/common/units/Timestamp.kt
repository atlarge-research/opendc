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

@file:OptIn(InternalUse::class)

package org.opendc.common.units

import kotlinx.serialization.Serializable
import org.opendc.common.annotations.InternalUse
import org.opendc.common.units.TimeDelta.Companion.toTimeDelta
import org.opendc.common.utils.ifNeg0thenPos0
import java.time.Duration
import java.time.Instant
import javax.naming.OperationNotSupportedException

/**
 * Represents timestamp values.
 * @see[Unit]
 */
@JvmInline
@Serializable(with = Timestamp.Companion.TimeStampSerializer::class)
public value class Timestamp private constructor(
    // In milliseconds since Epoch.
    public override val value: Double,
) : Unit<Timestamp> {
    @InternalUse
    override fun new(value: Double): Timestamp = Timestamp(value.ifNeg0thenPos0())

    @Deprecated(
        message = "operation not permitted on Timestamps, likely to be undesired operation",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("+ other.toIntervalFromEpoch()"),
    )
    override fun plus(other: Timestamp): Timestamp = throw OperationNotSupportedException()

    public operator fun plus(other: TimeDelta): Timestamp = ofEpochMs(value + other.toMs())

    @Deprecated(
        message = "operation not permitted on Timestamps, likely to be undesired operation",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("- other.toIntervalFromEpoch()"),
    )
    override fun minus(other: Timestamp): Timestamp = throw OperationNotSupportedException()

    public operator fun minus(other: TimeDelta): Timestamp = ofEpochMs(value - other.toMs())

    /**
     * @return the [TimeDelta] between *this* and [other]. Be aware that this is not the absolute value, it can be negative.
     */
    public infix fun timeDelta(other: Timestamp): TimeDelta = this.toTimeDeltaFromEpoch() - other.toTimeDeltaFromEpoch()

    public fun toEpochNs(): Double = value * 1e6

    public fun toEpochMicros(): Double = value * 1e3

    public fun toEpochMs(): Double = value

    public fun toEpochSec(): Double = value / 1000.0

    public fun toEpochMin(): Double = toEpochSec() / 60

    public fun toEpochHours(): Double = toEpochMin() / 60

    public fun toEpochDays(): Double = toEpochHours() / 24

    public fun toInstant(): Instant =
        if (toEpochMs() > Long.MAX_VALUE) {
            Instant.ofEpochSecond(toEpochSec().toLong())
        } else {
            Instant.ofEpochMilli(toEpochMs().toLong())
        }

    public fun toTimeDeltaFromEpoch(): TimeDelta = TimeDelta.ofMillis(toEpochMs())

    override fun toString(): String = fmtValue()

    /**
     * @return the [Instant] [toString] result of this [Timestamp] value.
     * @param[fmt] no ops.
     */
    override fun fmtValue(fmt: String): String = toInstant().toString()

    public companion object {
        @JvmStatic public val ZERO: Timestamp = Timestamp(.0)

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

        /**
         * Serializer for [Timestamp] value class. It needs to be a compile
         * time constant in order to be used as serializer automatically,
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
