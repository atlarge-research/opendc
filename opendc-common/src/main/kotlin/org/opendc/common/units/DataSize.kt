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
import java.time.Duration

/**
 * Represents data size value.
 * @param[value] the stored value in Bytes.
 * @see[Unit]
 */
@JvmInline
@Serializable(with = DataSize.Companion.DataSerializer::class)
public value class DataSize private constructor(
    override val value: Double,
) : Unit<DataSize> {
    @InternalUse
    override fun new(value: Double): DataSize = DataSize(value)

    public fun toBytes(): Double = value

    public fun toKiB(): Double = value / 1024

    public fun toKB(): Double = value / 1e3

    public fun toMiB(): Double = toKiB() / 1024

    public fun toMB(): Double = toKB() / 1e3

    public fun toGiB(): Double = toMiB() / 1024

    public fun toGB(): Double = toMB() / 1e3

    public fun toTiB(): Double = toGiB() / 1024

    public fun toTB(): Double = toGB() / 1e3

    override fun toString(): String = fmtValue()

    override fun fmtValue(fmt: String): String {
        val bytes100 = 1.0 / 1024 * 100
        val kB100 = 100.0
        val mB100 = 100.0 * 1024

        return when (value) {
            in (Double.MIN_VALUE..bytes100) -> "${toBytes().fmt(fmt)} Bytes"
            in (bytes100..kB100) -> "${toKiB().fmt(fmt)} KiB"
            in (kB100..mB100) -> "${toMiB().fmt(fmt)} MiB"
            else -> "${toGiB().fmt(fmt)} GiB"
        }
    }

    public operator fun div(time: Time): DataRate = DataRate.ofKBps(this.toKiB() / time.toSec())

    public operator fun div(duration: Duration): DataRate = this / duration.toTime()

    public companion object {
        @JvmStatic public val ZERO: DataSize = DataSize(.0)

        @JvmStatic
        @JvmName("ofBytes")
        public fun ofBytes(bytes: Number): DataSize = DataSize(bytes.toDouble())

        @JvmStatic
        @JvmName("ofKiB")
        public fun ofKiB(kiB: Number): DataSize = ofBytes(kiB.toDouble() * 1024)

        @JvmStatic
        @JvmName("ofKB")
        public fun ofKB(kB: Number): DataSize = ofBytes(kB.toDouble() * 1e3)

        @JvmStatic
        @JvmName("ofMiB")
        public fun ofMiB(miB: Number): DataSize = ofKiB(miB.toDouble() * 1024)

        @JvmStatic
        @JvmName("ofMB")
        public fun ofMB(mB: Number): DataSize = ofKB(mB.toDouble() * 1e3)

        @JvmStatic
        @JvmName("ofGiB")
        public fun ofGiB(giB: Number): DataSize = ofMiB(giB.toDouble() * 1024)

        @JvmStatic
        @JvmName("ofGB")
        public fun ofGB(gB: Number): DataSize = ofMB(gB.toDouble() * 1e3)

        @JvmStatic
        @JvmName("ofTiB")
        public fun ofTiB(tiB: Number): DataSize = ofGiB(tiB.toDouble() * 1024)

        @JvmStatic
        @JvmName("ofTB")
        public fun ofTB(tB: Number): DataSize = ofGB(tB.toDouble() * 1e3)

        private val bytesReg = Regex("\\s*([\\de.-]+)\\s*(?:B|Bytes)\\s*")
        private val kiBReg = Regex("\\s*([\\de.-]+)\\s*(?:KiB|KiBytes)\\s*")
        private val kBReg = Regex("\\s*([\\de.-]+)\\s*(?:KB|KBytes)\\s*")
        private val miBReg = Regex("\\s*([\\de.-]+)\\s*(?:MiB|MiBytes)\\s*")
        private val mBReg = Regex("\\s*([\\de.-]+)\\s*(?:MB|MBytes)\\s*")
        private val giBReg = Regex("\\s*([\\de.-]+)\\s*(?:GiB|GiBytes)\\s*")
        private val gBReg = Regex("\\s*([\\de.-]+)\\s*(?:GB|GBytes)\\s*")
        private val tiBReg = Regex("\\s*([\\de.-]+)\\s*(?:TiB|TiBytes)\\s*")
        private val tBReg = Regex("\\s*([\\de.-]+)\\s*(?:TB|TBytes)\\s*")

        /**
         * Serializer for [DataSize] value class. It needs to be a compile
         * time constant in order to be used as serializer automatically,
         * hence `object :` instead of class instantiation.
         *
         * ```json
         * // e.g.
         * "data": "100GB"
         * "data": "  1    MB   "
         * // etc.
         * ```
         */
        internal object DataSerializer : UnitSerializer<DataSize>(
            ifNumber = {
                LOG.warn(
                    "deserialization of number with no unit of measure for unit 'DataSize', " +
                        "assuming it is in Mib. Keep in mind that you can also specify the value as '$it Mib'",
                )
                ofMiB(it.toDouble())
            },
            serializerFun = { this.encodeString(it.toString()) },
            ifMatches(bytesReg) { ofBytes(json.decNumFromStr(groupValues[1])) },
            ifMatches(kiBReg) { ofKiB(json.decNumFromStr(groupValues[1])) },
            ifMatches(kBReg) { ofKB(json.decNumFromStr(groupValues[1])) },
            ifMatches(miBReg) { ofMiB(json.decNumFromStr(groupValues[1])) },
            ifMatches(mBReg) { ofMB(json.decNumFromStr(groupValues[1])) },
            ifMatches(giBReg) { ofGiB(json.decNumFromStr(groupValues[1])) },
            ifMatches(gBReg) { ofGB(json.decNumFromStr(groupValues[1])) },
            ifMatches(tiBReg) { ofTiB(json.decNumFromStr(groupValues[1])) },
            ifMatches(tBReg) { ofTB(json.decNumFromStr(groupValues[1])) },
        )
    }
}
