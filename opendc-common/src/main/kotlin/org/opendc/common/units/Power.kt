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
 * Represents power values.
 * @see[Unit]
 */
@JvmInline
@Serializable(with = Power.Companion.PowerSerializer::class)
public value class Power private constructor(
    // In Watts.
    override val value: Double,
) : Unit<Power> {
    @InternalUse
    override fun new(value: Double): Power = Power(value.ifNeg0thenPos0())

    public fun toWatts(): Double = value

    public fun toKWatts(): Double = value / 1000.0

    override fun toString(): String = fmtValue()

    override fun fmtValue(fmt: String): String =
        if (value >= 1000.0) {
            "${toKWatts().fmt(fmt)} KWatts"
        } else {
            "${toWatts().fmt(fmt)} Watts"
        }

    public operator fun times(timeDelta: TimeDelta): Energy = Energy.ofWh(toWatts() * timeDelta.toHours())

    public operator fun times(duration: Duration): Energy = this * duration.toTimeDelta()

    public companion object {
        @JvmStatic
        public val ZERO: Power = Power(.0)

        @JvmStatic
        @JvmName("ofWatts")
        public fun ofWatts(watts: Number): Power = Power(watts.toDouble())

        @JvmStatic
        @JvmName("ofKWatts")
        public fun ofKWatts(kWatts: Number): Power = Power(kWatts.toDouble() * 1000.0)

        /**
         * Serializer for [Power] value class. It needs to be a compile
         * time constant in order to be used as serializer automatically,
         * hence `object :` instead of class instantiation.
         *
         * ```json
         * // e.g.
         * "power-draw": "4 watts"
         * "power-draw": "  1    KWatt   "
         * // etc.
         * ```
         */
        internal object PowerSerializer : UnitSerializer<Power>(
            ifNumber = {
                LOG.warn(
                    "deserialization of number with no unit of measure, assuming it is in Watts." +
                        "Keep in mind that you can also specify the value as '$it W'",
                )
                ofWatts(it.toDouble())
            },
            serializerFun = { this.encodeString(it.toString()) },
            ifMatches("$NUM_GROUP$WATTS", IGNORE_CASE) { ofWatts(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$KILO$WATTS", IGNORE_CASE) { ofKWatts(json.decNumFromStr(groupValues[1])) },
        )
    }
}
