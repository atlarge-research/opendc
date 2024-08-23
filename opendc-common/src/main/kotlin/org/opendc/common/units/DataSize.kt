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
import java.time.Duration

/**
 * Represents data size value.
 * @see[Unit]
 */
@JvmInline
@Serializable(with = DataSize.Companion.DataSerializer::class)
public value class DataSize private constructor(
    // In MiB.
    override val value: Double,
) : Unit<DataSize> {
    @InternalUse
    override fun new(value: Double): DataSize = DataSize(value)

    public fun toBits(): Double = toKib() * 1024

    public fun toBytes(): Double = toKiB() * 1024

    // Metric prefixes.

    public fun toKb(): Double = toBits() / 1e3

    public fun toKB(): Double = toBytes() / 1e3

    public fun toMb(): Double = toKb() / 1e3

    public fun toMB(): Double = toKB() / 1e3

    public fun toGb(): Double = toMb() / 1e3

    public fun toGB(): Double = toMB() / 1e3

    public fun toTb(): Double = toGb() / 1e3

    public fun toTB(): Double = toGB() / 1e3

    // Binary prefixes.

    public fun toKib(): Double = toMib() * 1024

    public fun toKiB(): Double = toMiB() * 1024

    public fun toMib(): Double = toMiB() * 8

    public fun toMiB(): Double = value

    public fun toGib(): Double = toMib() / 1024

    public fun toGiB(): Double = toMiB() / 1024

    public fun toTib(): Double = toGib() / 1024

    public fun toTiB(): Double = toGiB() / 1024

    override fun toString(): String = fmtValue()

    override fun fmtValue(fmt: String): String =
        when (abs()) {
            in ZERO..ofBytes(100) -> "${toBytes().fmt(fmt)} Bytes"
            in ofBytes(100)..ofKiB(100) -> "${toKiB().fmt(fmt)} KiB"
            in ofKiB(100)..ofMiB(100) -> "${toMiB().fmt(fmt)} MiB"
            else -> "${toGiB().fmt(fmt)} GiB"
        }

    public operator fun div(timeDelta: TimeDelta): DataRate = DataRate.ofKBps(this.toKiB() / timeDelta.toSec())

    public operator fun div(duration: Duration): DataRate = this / duration.toTimeDelta()

    public companion object {
        @JvmStatic public val ZERO: DataSize = DataSize(.0)

        @JvmStatic
        @JvmName("ofBits")
        public fun ofBits(bits: Number): DataSize = ofKib(bits.toDouble() / 1024)

        @JvmStatic
        @JvmName("ofBytes")
        public fun ofBytes(bytes: Number): DataSize = ofKiB(bytes.toDouble() / 1024)

        // Metric prefixes.

        @JvmStatic
        @JvmName("ofKb")
        public fun ofKb(kb: Number): DataSize = ofBits(kb.toDouble() * 1e3)

        @JvmStatic
        @JvmName("ofKB")
        public fun ofKB(kB: Number): DataSize = ofBytes(kB.toDouble() * 1e3)

        @JvmStatic
        @JvmName("ofMb")
        public fun ofMb(mb: Number): DataSize = ofKb(mb.toDouble() * 1e3)

        @JvmStatic
        @JvmName("ofMB")
        public fun ofMB(mB: Number): DataSize = ofKB(mB.toDouble() * 1e3)

        @JvmStatic
        @JvmName("ofGb")
        public fun ofGb(gb: Number): DataSize = ofMb(gb.toDouble() * 1e3)

        @JvmStatic
        @JvmName("ofGB")
        public fun ofGB(gB: Number): DataSize = ofMB(gB.toDouble() * 1e3)

        @JvmStatic
        @JvmName("ofTb")
        public fun ofTb(tb: Number): DataSize = ofGb(tb.toDouble() * 1e3)

        @JvmStatic
        @JvmName("ofTB")
        public fun ofTB(tB: Number): DataSize = ofGB(tB.toDouble() * 1e3)

        // Binary prefixes.

        @JvmStatic
        @JvmName("ofKib")
        public fun ofKib(kib: Number): DataSize = ofMib(kib.toDouble() / 1024)

        @JvmStatic
        @JvmName("ofKiB")
        public fun ofKiB(kiB: Number): DataSize = ofMiB(kiB.toDouble() / 1024)

        @JvmStatic
        @JvmName("ofMib")
        public fun ofMib(mib: Number): DataSize = ofMiB(mib.toDouble() / 8)

        @JvmStatic
        @JvmName("ofMiB")
        public fun ofMiB(miB: Number): DataSize = DataSize(miB.toDouble())

        @JvmStatic
        @JvmName("ofGib")
        public fun ofGib(gib: Number): DataSize = ofMib(gib.toDouble() * 1024)

        @JvmStatic
        @JvmName("ofGiB")
        public fun ofGiB(giB: Number): DataSize = ofMiB(giB.toDouble() * 1024)

        @JvmStatic
        @JvmName("ofTib")
        public fun ofTib(tib: Number): DataSize = ofGib(tib.toDouble() * 1024)

        @JvmStatic
        @JvmName("ofTiB")
        public fun ofTiB(tiB: Number): DataSize = ofGiB(tiB.toDouble() * 1024)

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
                        "assuming it is in MiB. Keep in mind that you can also specify the value as '$it MiB'",
                )
                ofMiB(it.toDouble())
            },
            serializerFun = { this.encodeString(it.toString()) },
            ifMatches("$NUM_GROUP$BITS") { ofBits(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$BYTES") { ofBytes(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$KIBI$BITS") { ofKib(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$KILO$BITS") { ofKb(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$KIBI$BYTES") { ofKiB(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$KILO$BYTES") { ofKB(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$MEBI$BITS") { ofMib(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$MEGA$BITS") { ofMb(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$MEBI$BYTES") { ofMiB(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$MEGA$BYTES") { ofMB(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$GIBI$BITS") { ofGib(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$GIGA$BITS") { ofGb(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$GIBI$BYTES") { ofGiB(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$GIGA$BYTES") { ofGB(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$TEBI$BITS") { ofTib(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$TERA$BITS") { ofTb(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$TEBI$BYTES") { ofTiB(json.decNumFromStr(groupValues[1])) },
            ifMatches("$NUM_GROUP$TERA$BYTES") { ofTB(json.decNumFromStr(groupValues[1])) },
        )
    }
}
