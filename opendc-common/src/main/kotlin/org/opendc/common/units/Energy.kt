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
    override fun new(value: Double): Energy = Energy(value.ifNeg0thenPos0())

    public fun toJoule(): Double = value

    public fun toKJoule(): Double = value / 1000

    public fun toWh(): Double = value / 3600

    public fun toKWh(): Double = toWh() / 1000

    override fun toString(): String = fmtValue()

    override fun fmtValue(fmt: String): String =
        if (value >= 1000.0) {
            "${toJoule().fmt(fmt)} Joule"
        } else {
            "${toKJoule().fmt(fmt)} KJoule"
        }

    public operator fun div(timeDelta: TimeDelta): Power = Power.ofWatts(toWh() / timeDelta.toHours())

    public operator fun div(duration: Duration): Power = this / duration.toTimeDelta()

    public companion object {
        @JvmStatic
        public val ZERO: Energy = Energy(.0)

        @JvmStatic
        @JvmName("ofJoule")
        public fun ofJoule(joule: Number): Energy = Energy(joule.toDouble())

        @JvmStatic
        @JvmName("ofKJoule")
        public fun ofKJoule(kJoule: Number): Energy = ofJoule(kJoule.toDouble() * 1000)

        @JvmStatic
        @JvmName("ofWh")
        public fun ofWh(wh: Number): Energy = ofJoule(wh.toDouble() * 3600)

        @JvmStatic
        @JvmName("ofKWh")
        public fun ofKWh(kWh: Number): Energy = ofWh(kWh.toDouble() * 1000.0)

        private val JOULES = Regex("\\s*(?:j|(?:joule|Joule)(?:|s))")

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
            ifMatches("$NUM_GROUP$WATTS$PER$HOUR", IGNORE_CASE) { ofWh(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$KILO$WATTS$PER$HOUR", IGNORE_CASE) { ofKWh(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$JOULES", IGNORE_CASE) { ofJoule(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$KILO$JOULES", IGNORE_CASE) { ofKJoule(json.decNumFromStr(groupValues[1])) },
        )
    }
}
