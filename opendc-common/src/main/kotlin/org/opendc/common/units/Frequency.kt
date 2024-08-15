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
    override val value: Double,
) : Unit<Frequency> {
    override fun new(value: Double): Frequency = Frequency(value.ifNeg0thenPos0().also { check(it >= .0) })

    public fun toHz(): Double = value

    public fun toKHz(): Double = value / 1e-3

    public fun toMHz(): Double = value / 1e-6

    public fun toGHz(): Double = value / 1e-9

    override fun toString(): String = fmtValue()

    override fun fmtValue(fmt: String): String {
        val hz100 = 100.0
        val khz100 = 100e3
        val mhz100 = 100e6

        return when (value) {
            in Double.MIN_VALUE..hz100 -> "${toHz().fmt(fmt)} Hz"
            in hz100..khz100 -> "${toKHz().fmt(fmt)} KHz"
            in khz100..mhz100 -> "${toMHz().fmt(fmt)} MHz"
            else -> "${toGHz().fmt(fmt)} GHz"
        }
    }

    public operator fun times(time: Time): Double = this.value * time.toSec()

    public operator fun times(duration: Duration): Double = this.value * duration.toSeconds()

    public companion object {
        @JvmStatic public val ZERO: Frequency = Frequency(.0)

        @JvmStatic
        @JvmName("ofHz")
        public fun ofHz(hz: Number): Frequency = Frequency(hz.toDouble())

        @JvmStatic
        @JvmName("ofKHz")
        public fun ofKHz(kHz: Number): Frequency = Frequency(kHz.toDouble() * 1e3)

        @JvmStatic
        @JvmName("ofMHz")
        public fun ofMHz(mHz: Number): Frequency = Frequency(mHz.toDouble() * 1e6)

        @JvmStatic
        @JvmName("ofGHz")
        public fun ofGHz(gHz: Number): Frequency = Frequency(gHz.toDouble() * 1e9)

        private val hzRegex = Regex("\\s*([\\de.-]+)\\s*(?:Hz|Hertz)\\s*", IGNORE_CASE)
        private val kHzRegex = Regex("\\s*([\\de.-]+)\\s*(?:K|Kilo)(?:Hz|Hertz)\\s*", IGNORE_CASE)
        private val mHzRegex = Regex("\\s*([\\de.-]+)\\s*(?:M|Mega)(?:Hz|Hertz)\\s*", IGNORE_CASE)
        private val gHzRegex = Regex("\\s*([\\de.-]+)\\s*(?:G|Giga)(?:Hz|Hertz)\\s*", IGNORE_CASE)

        /**
         * Serializer for [Frequency] value class. It needs to be a compile
         * time constant in order to be used as serializer automatically,
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
                LOG.warn("deserialization of number with no unit of measure, assuming it is in Hertz...")
                ofHz(it.toDouble())
            },
            serializerFun = { this.encodeString(it.toString()) },
            ifMatches(hzRegex) { ofHz(json.decNumFromStr(groupValues[1])) },
            ifMatches(kHzRegex) { ofKHz(json.decNumFromStr(groupValues[1])) },
            ifMatches(mHzRegex) { ofMHz(json.decNumFromStr(groupValues[1])) },
            ifMatches(gHzRegex) { ofGHz(json.decNumFromStr(groupValues[1])) },
        )
    }
}
