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
    @InternalUse
    override fun new(value: Double): TimeDelta = TimeDelta(value.ifNeg0thenPos0())

    public fun toNs(): Double = value * 1e6

    public fun toMicros(): Double = value * 1e3

    public fun toMs(): Double = value

    public fun toMsLong(): Long = value.toLong()

    public fun toSec(): Double = value / 1000.0

    public fun toMin(): Double = toSec() / 60

    public fun toHours(): Double = toMin() / 60

    public fun toInstantFromEpoch(): Instant = Instant.ofEpochMilli(value.toLong())

    override fun toString(): String = fmtValue()

    /**
     * @return the [Duration] [toString] result of this time value.
     */
    override fun fmtValue(fmt: String): String = Duration.ofMillis(value.toLong()).toString()

    public operator fun times(power: Power): Energy = Energy.ofWh(toHours() * power.toWatts())

    public operator fun times(dataRate: DataRate): DataSize = DataSize.ofKB(toSec() * dataRate.toKBps())

    public companion object {
        @JvmStatic public val ZERO: TimeDelta = TimeDelta(.0)

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

        /**
         * Serializer for [TimeDelta] value class. It needs to be a compile
         * time constant in order to be used as serializer automatically,
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
