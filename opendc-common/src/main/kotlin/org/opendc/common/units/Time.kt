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
 * Represents time values.
 * @property[value] the time value in milliseconds.
 * @see[Unit]
 */
@JvmInline
@Serializable(with = Time.Companion.TimeSerializer::class)
public value class Time private constructor(
    public override val value: Double,
) : Unit<Time> {
    @InternalUse
    override fun new(value: Double): Time = Time(value.ifNeg0thenPos0())

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
        @JvmStatic public val ZERO: Time = Time(.0)

        @JvmStatic
        @JvmName("ofNanos")
        public fun ofNanos(nanos: Number): Time = Time(nanos.toDouble() / 1e6)

        @JvmStatic
        @JvmName("ofMicros")
        public fun ofMicros(micros: Number): Time = Time(micros.toDouble() / 1e3)

        @JvmStatic
        @JvmName("ofMillis")
        public fun ofMillis(ms: Number): Time = Time(ms.toDouble())

        @JvmStatic
        @JvmName("ofSec")
        public fun ofSec(sec: Number): Time = Time(sec.toDouble() * 1000.0)

        @JvmStatic
        @JvmName("ofMin")
        public fun ofMin(min: Number): Time = Time(min.toDouble() * 60 * 1000.0)

        @JvmStatic
        @JvmName("ofHours")
        public fun ofHours(hours: Number): Time = Time(hours.toDouble() * 60 * 60 * 1000.0)

        @JvmStatic
        @JvmName("ofDuration")
        public fun ofDuration(duration: Duration): Time = duration.toTime()

        @JvmStatic
        @JvmName("ofInstantFromEpoch")
        public fun ofInstantFromEpoch(instant: Instant): Time = ofMillis(instant.toEpochMilli())

        private val nanoReg = Regex("\\s*([\\de.-]+)\\s*(?:ns|nanos|nanosec|nanoseconds)\\s*", IGNORE_CASE)
        private val microReg = Regex("\\s*([\\de.-]+)\\s*(?:μs|micros|microsec|microseconds)\\s*", IGNORE_CASE)
        private val msReg = Regex("\\s*([\\de.-]+)\\s*(?:ms|millis|millisec|milliseconds)\\s*", IGNORE_CASE)
        private val secReg = Regex("\\s*([\\de.-]+)\\s*(?:sec|seconds)\\s*", IGNORE_CASE)
        private val minReg = Regex("\\s*([\\de.-]+)\\s*(?:min|minutes)\\s*", IGNORE_CASE)
        private val hoursReg = Regex("\\s*([\\de.-]+)\\s*(?:h|hours)\\s*", IGNORE_CASE)

        /**
         * Serializer for [Time] value class. It needs to be a compile
         * time constant in order to be used as serializer automatically,
         * hence `object :` instead of class instantiation.
         *
         * ```json
         * // e.g.
         * "time": "10 hours"
         * "time": "  30    minutes   "
         * "time": "1 ms"
         * "time": "PT13H"
         * // etc.
         * ```
         */
        internal object TimeSerializer : UnitSerializer<Time>(
            ifNumber = {
                LOG.warn(
                    "deserialization of number with no unit of measure, assuming it is in milliseconds." +
                        "Keep in mind that you can also specify the value as '$it ms'",
                )
                ofMillis(it.toDouble())
            },
            serializerFun = { this.encodeString(it.toString()) },
            ifMatches(nanoReg) { ofNanos(json.decNumFromStr(groupValues[1])) },
            ifMatches(microReg) { ofMicros(json.decNumFromStr(groupValues[1])) },
            ifMatches(msReg) { ofMillis(json.decNumFromStr(groupValues[1])) },
            ifMatches(secReg) { ofSec(json.decNumFromStr(groupValues[1])) },
            ifMatches(minReg) { ofMin(json.decNumFromStr(groupValues[1])) },
            ifMatches(hoursReg) { ofHours(json.decNumFromStr(groupValues[1])) },
            ifNoExc { ofDuration(Duration.parse(this)) },
            ifNoExc { ofInstantFromEpoch(Instant.parse(this)) },
        )

        /**
         * @return [this] converted to a [Time] value, with the highest possible accuracy.
         *
         * @throws RuntimeException if [this] cannot be represented as nanos, millis, seconds, minutes or hours with a [Long].
         */
        public fun Duration.toTime(): Time {
            fun tryNoThrow(block: () -> Time?) =
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
                    "duration $this cannot be converted to ${Time::class.simpleName}, " +
                        "duration value overflow Long representation of nanos, millis, seconds, minutes or hours",
                )
        }
    }
}
