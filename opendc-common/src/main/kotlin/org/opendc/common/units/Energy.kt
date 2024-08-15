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
import org.opendc.common.units.Time.Companion.toTime
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
    override val value: Double,
) : Unit<Energy> {
    override fun new(value: Double): Energy = Energy(value.ifNeg0thenPos0())

    public fun toWh(): Double = value

    public fun toKWh(): Double = value / 1000

    public fun toJoule(): Double = value * 3600

    public fun toKJoule(): Double = value * 3600 / 1000

    override fun toString(): String = fmtValue()

    override fun fmtValue(fmt: String): String =
        if (value >= 1000.0) {
            "${toKWh().fmt(fmt)} KWh"
        } else {
            "${toWh().fmt(fmt)} Wh"
        }

    public operator fun div(time: Time): Power = Power.ofWatts(toWh() / time.toHours())

    public operator fun div(duration: Duration): Power = this / duration.toTime()

    public companion object {
        @JvmStatic
        public val ZERO: Energy = Energy(.0)

        @JvmStatic
        @JvmName("ofWh")
        public fun ofWh(wh: Number): Energy = Energy(wh.toDouble())

        @JvmStatic
        @JvmName("ofKWh")
        public fun ofKWh(kWh: Number): Energy = Energy(kWh.toDouble() * 1000.0)

        @JvmStatic
        @JvmName("ofJoule")
        public fun ofJoule(joule: Number): Energy = Energy(joule.toDouble() / 3600)

        @JvmStatic
        @JvmName("ofKJoule")
        public fun ofKJoule(joule: Number): Energy = Energy(joule.toDouble() * 1000 / 3600)

        private val whReg = Regex("\\s*([\\de.-]+)\\s*(?:w|watt|watts)-*(?:h|hour|hours)\\s*", IGNORE_CASE)
        private val kWhReg = Regex("\\s*([\\de.-]+)\\s*(?:k|kilo)\\s*(?:w|watt|watts)-*(?:h|hour|hours)\\s*", IGNORE_CASE)
        private val jouleReg = Regex("\\s*([\\de.-]+)\\s*(?:j|joule|joules)\\s*", IGNORE_CASE)
        private val kJouleReg = Regex("\\s*([\\de.-]+)\\s*(?:k|kilo)\\s*(?:j|joule|joules)\\s*", IGNORE_CASE)

        /**
         * Serializer for [Energy] value class. It needs to be a compile
         * time constant in order to be used as serializer automatically,
         * hence `object :` instead of class instantiation.
         *
         * ```json
         * // e.g.
         * "energy": "1 KWh"
         * "energy": "  3    watts-hour  "
         * "energy": "10.5 Joules"
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
            serializerFun = { this.encodeString(it.toString()) },
            ifMatches(whReg) { ofWh(json.decNumFromStr(groupValues[1])) },
            ifMatches(kWhReg) { ofKWh(json.decNumFromStr(groupValues[1])) },
            ifMatches(jouleReg) { ofJoule(json.decNumFromStr(groupValues[1])) },
            ifMatches(kJouleReg) { ofKJoule(json.decNumFromStr(groupValues[1])) },
        )
    }
}
