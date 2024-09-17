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

/**
 * Represents data-rate values.
 * @see[Unit]
 */
@JvmInline
@Serializable(with = DataRate.Companion.DataRateSerializer::class)
public value class DataRate private constructor(
    // In bits/s.
    override val value: Double,
) : Unit<DataRate> {
    @InternalUse
    override fun new(value: Double): DataRate = DataRate(value.ifNeg0thenPos0())

    public fun tobps(): Double = value

    public fun toKibps(): Double = value / 1024

    public fun toKbps(): Double = value / 1e3

    public fun toKiBps(): Double = toKibps() / 8

    public fun toKBps(): Double = toKbps() / 8

    public fun toMibps(): Double = toKibps() / 1024

    public fun toMbps(): Double = toKbps() / 1e3

    public fun toMiBps(): Double = toMibps() / 8

    public fun toMBps(): Double = toMbps() / 8

    public fun toGibps(): Double = toMibps() / 1024

    public fun toGbps(): Double = toMbps() / 1e3

    public fun toGiBps(): Double = toGibps() / 8

    public fun toGBps(): Double = toGbps() / 8

    override fun toString(): String = fmtValue()

    public override fun fmtValue(fmt: String): String =
        when (abs()) {
            in ZERO..ofBps(100) -> "${String.format(fmt, tobps())} bps"
            in ofbps(100)..ofKbps(100) -> "${String.format(fmt, toKbps())} Kbps"
            in ofKbps(100)..ofMbps(100) -> "${String.format(fmt, toMbps())} Mbps"
            else -> "${String.format(fmt, toGbps())} Gbps"
        }

    public operator fun times(timeDelta: TimeDelta): DataSize = DataSize.ofKiB(toKiBps() * timeDelta.toSec())

    public operator fun times(duration: Duration): DataSize = this * duration.toTimeDelta()

    public companion object {
        @JvmStatic public val ZERO: DataRate = DataRate(.0)

        @JvmStatic
        @JvmName("ofbps")
        public fun ofbps(bps: Number): DataRate = DataRate(bps.toDouble())

        @JvmStatic
        @JvmName("ofBps")
        public fun ofBps(Bps: Number): DataRate = ofbps(Bps.toDouble() * 8)

        @JvmStatic
        @JvmName("ofKibps")
        public fun ofKibps(kibps: Number): DataRate = ofbps(kibps.toDouble() * 1024)

        @JvmStatic
        @JvmName("ofKbps")
        public fun ofKbps(kbps: Number): DataRate = ofbps(kbps.toDouble() * 1e3)

        @JvmStatic
        @JvmName("ofKiBps")
        public fun ofKiBps(kiBps: Number): DataRate = ofKibps(kiBps.toDouble() * 8)

        @JvmStatic
        @JvmName("ofKBps")
        public fun ofKBps(kBps: Number): DataRate = ofKbps(kBps.toDouble() * 8)

        @JvmStatic
        @JvmName("ofMibps")
        public fun ofMibps(mibps: Number): DataRate = ofKibps(mibps.toDouble() * 1024)

        @JvmStatic
        @JvmName("ofMbps")
        public fun ofMbps(mbps: Number): DataRate = ofKbps(mbps.toDouble() * 1e3)

        @JvmStatic
        @JvmName("ofMiBps")
        public fun ofMiBps(miBps: Number): DataRate = ofMibps(miBps.toDouble() * 8)

        @JvmStatic
        @JvmName("ofMBps")
        public fun ofMBps(mBps: Number): DataRate = ofMbps(mBps.toDouble() * 8)

        @JvmStatic
        @JvmName("ofGibps")
        public fun ofGibps(gibps: Number): DataRate = ofMibps(gibps.toDouble() * 1024)

        @JvmStatic
        @JvmName("ofGbps")
        public fun ofGbps(gbps: Number): DataRate = ofMbps(gbps.toDouble() * 1e3)

        @JvmStatic
        @JvmName("ofGiBps")
        public fun ofGiBps(giBps: Number): DataRate = ofGibps(giBps.toDouble() * 8)

        @JvmStatic
        @JvmName("ofGBps")
        public fun ofGBps(gBps: Number): DataRate = ofGbps(gBps.toDouble() * 8)

        /**
         * Serializer for [DataRate] value class. It needs to be a compile
         * time constant in order to be used as serializer automatically,
         * hence `object :` instead of class instantiation.
         *
         * ```json
         * // e.g.
         * "data-rate": "1 Gbps"
         * "data-rate": "10KBps"
         * "data-rate": "   0.3    GBps  "
         * // etc.
         * ```
         */
        internal object DataRateSerializer : UnitSerializer<DataRate>(
            ifNumber = {
                LOG.warn(
                    "deserialization of number with no unit of measure, assuming it is in Kibps." +
                        "Keep in mind that you can also specify the value as '$it Kibps'",
                )
                ofKibps(it.toDouble())
            },
            serializerFun = { this.encodeString(it.toString()) },
            ifMatches("$NUM_GROUP$BITS$PER$SEC") { ofbps(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$BYTES$PER$SEC") { ofBps(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$KIBI$BITS$PER$SEC") { ofKibps(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$KILO$BITS$PER$SEC") { ofKbps(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$KIBI$BYTES$PER$SEC") { ofKiBps(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$KILO$BYTES$PER$SEC") { ofKBps(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$MEBI$BITS$PER$SEC") { ofMibps(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$MEGA$BITS$PER$SEC") { ofMbps(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$MEBI$BYTES$PER$SEC") { ofMiBps(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$MEGA$BYTES$PER$SEC") { ofMBps(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$GIBI$BITS$PER$SEC") { ofGibps(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$GIGA$BITS$PER$SEC") { ofGbps(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$GIBI$BYTES$PER$SEC") { ofGiBps(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$GIGA$BYTES$PER$SEC") { ofGBps(json.decNumFromStr(groupValues[1])) },
        )
    }
}
