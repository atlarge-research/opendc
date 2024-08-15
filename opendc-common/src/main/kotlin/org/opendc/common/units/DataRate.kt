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
import org.opendc.common.utils.ifNeg0thenPos0
import java.time.Duration

/**
 * Represents data-rate values.
 * @param[value] the store value in bits/s.
 * @see[Unit]
 */
@JvmInline
@Serializable(with = DataRate.Companion.DataRateSerializer::class)
public value class DataRate private constructor(
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

    public override fun fmtValue(fmt: String): String {
        val bps100 = 100.0
        val kibps100 = bps100 * 1024
        val mibps100 = kibps100 * 1024

        return when (value) {
            in Double.MIN_VALUE..bps100 -> "${String.format(fmt, tobps())} bps"
            in bps100..kibps100 -> "${String.format(fmt, toKibps())} Kibps"
            in kibps100..mibps100 -> "${String.format(fmt, toMibps())} Mibps"
            else -> "${String.format(fmt, toGibps())} Gibps"
        }
    }

    public operator fun times(time: Time): DataSize = DataSize.ofKiB(toKiBps() * time.toSec())

    public operator fun times(duration: Duration): DataSize = this * duration.toTime()

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

        private val bitsPsReg = Regex("\\s*([\\de.-]+)\\s*bps\\s*")
        private val bytesPsReg = Regex("\\s*([\\de.-]+)\\s*Bps\\s*")
        private val kibpsReg = Regex("\\s*([\\de.-]+)\\s*(?:Kibps|kibps)\\s*")
        private val kbpsReg = Regex("\\s*([\\de.-]+)\\s*(?:Kbps|kbps)\\s*")
        private val kiBpsReg = Regex("\\s*([\\de.-]+)\\s*(?:KiBps|kiBps)\\s*")
        private val kBpsReg = Regex("\\s*([\\de.-]+)\\s*(?:KBps|kBps)\\s*")
        private val mibpsReg = Regex("\\s*([\\de.-]+)\\s*(?:Mibps|mibps)\\s*")
        private val mbpsReg = Regex("\\s*([\\de.-]+)\\s*(?:Mbps|mbps)\\s*")
        private val miBpsReg = Regex("\\s*([\\de.-]+)\\s*(?:MiBps|miBps)\\s*")
        private val mBpsReg = Regex("\\s*([\\de.-]+)\\s*(?:MBps|mBps)\\s*")
        private val gibpsReg = Regex("\\s*([\\de.-]+)\\s*(?:Gibps|gibps)\\s*")
        private val gbpsReg = Regex("\\s*([\\de.-]+)\\s*(?:Gbps|gbps)\\s*")
        private val giBpsReg = Regex("\\s*([\\de.-]+)\\s*(?:GiBps|giBps)\\s*")
        private val gBpsReg = Regex("\\s*([\\de.-]+)\\s*(?:GBps|gBps)\\s*")

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
            ifMatches(bitsPsReg) { ofbps(json.decNumFromStr(groupValues[1])) },
            ifMatches(bytesPsReg) { ofBps(json.decNumFromStr(groupValues[1])) },
            ifMatches(kibpsReg) { ofKibps(json.decNumFromStr(groupValues[1])) },
            ifMatches(kbpsReg) { ofKbps(json.decNumFromStr(groupValues[1])) },
            ifMatches(kiBpsReg) { ofKiBps(json.decNumFromStr(groupValues[1])) },
            ifMatches(kBpsReg) { ofKBps(json.decNumFromStr(groupValues[1])) },
            ifMatches(mibpsReg) { ofMibps(json.decNumFromStr(groupValues[1])) },
            ifMatches(mbpsReg) { ofMbps(json.decNumFromStr(groupValues[1])) },
            ifMatches(miBpsReg) { ofMiBps(json.decNumFromStr(groupValues[1])) },
            ifMatches(mBpsReg) { ofMBps(json.decNumFromStr(groupValues[1])) },
            ifMatches(gibpsReg) { ofGibps(json.decNumFromStr(groupValues[1])) },
            ifMatches(gbpsReg) { ofGbps(json.decNumFromStr(groupValues[1])) },
            ifMatches(giBpsReg) { ofGiBps(json.decNumFromStr(groupValues[1])) },
            ifMatches(gBpsReg) { ofGBps(json.decNumFromStr(groupValues[1])) },
        )
    }
}
