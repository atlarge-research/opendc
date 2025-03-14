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

@file:OptIn(InternalUse::class, NonInlinableUnit::class)

package org.opendc.common.units

import kotlinx.serialization.Serializable
import org.opendc.common.annotations.InternalUse
import org.opendc.common.units.TimeDelta.Companion.toTimeDelta
import org.opendc.common.utils.DFLT_MIN_EPS
import org.opendc.common.utils.approx
import org.opendc.common.utils.approxLarger
import org.opendc.common.utils.approxLargerOrEq
import org.opendc.common.utils.approxSmaller
import org.opendc.common.utils.approxSmallerOrEq
import org.opendc.common.utils.fmt
import org.opendc.common.utils.ifNeg0thenPos0
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
    override fun toString(): String = fmtValue()

    override fun fmtValue(fmt: String): String =
        when (abs()) {
            in zero..ofBytes(100) -> "${toBytes().fmt(fmt)} Bytes"
            in ofBytes(100)..ofKiB(100) -> "${toKiB().fmt(fmt)} KiB"
            in ofKiB(100)..ofMiB(100) -> "${toMiB().fmt(fmt)} MiB"
            else -> "${toGiB().fmt(fmt)} GiB"
        }

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Conversions to Double
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Operation Override (to avoid boxing of value classes in byte code)
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public override fun ifNeg0ThenPos0(): DataSize = DataSize(value.ifNeg0thenPos0())

    public override operator fun plus(other: DataSize): DataSize = DataSize(value + other.value)

    public override operator fun minus(other: DataSize): DataSize = DataSize(value - other.value)

    public override operator fun div(scalar: Number): DataSize = DataSize(value / scalar.toDouble())

    public override operator fun div(other: DataSize): Percentage = Percentage.ofRatio(value / other.value)

    public override operator fun times(scalar: Number): DataSize = DataSize(value * scalar.toDouble())

    public override operator fun times(percentage: Percentage): DataSize = DataSize(value * percentage.value)

    public override operator fun unaryMinus(): DataSize = DataSize(-value)

    public override operator fun compareTo(other: DataSize): Int = this.value.compareTo(other.value)

    public override fun isZero(): Boolean = value == .0

    public override fun approxZero(epsilon: Double): Boolean = value.approx(.0, epsilon = epsilon)

    public override fun approx(
        other: DataSize,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this == other || this.value.approx(other.value, minEpsilon, epsilon)

    public override infix fun approx(other: DataSize): Boolean = approx(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxLarger(
        other: DataSize,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxLarger(other.value, minEpsilon, epsilon)

    public override infix fun approxLarger(other: DataSize): Boolean = approxLarger(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxLargerOrEq(
        other: DataSize,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxLargerOrEq(other.value, minEpsilon, epsilon)

    public override infix fun approxLargerOrEq(other: DataSize): Boolean = approxLargerOrEq(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxSmaller(
        other: DataSize,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxSmaller(other.value, minEpsilon, epsilon)

    public override infix fun approxSmaller(other: DataSize): Boolean = approxSmaller(other, minEpsilon = DFLT_MIN_EPS)

    public override fun approxSmallerOrEq(
        other: DataSize,
        minEpsilon: Double,
        epsilon: Double,
    ): Boolean = this.value.approxSmallerOrEq(other.value, minEpsilon, epsilon)

    public override infix fun approxSmallerOrEq(other: DataSize): Boolean = approxSmallerOrEq(other, minEpsilon = DFLT_MIN_EPS)

    public override infix fun max(other: DataSize): DataSize = if (this.value > other.value) this else other

    public override infix fun min(other: DataSize): DataSize = if (this.value < other.value) this else other

    public override fun abs(): DataSize = DataSize(kotlin.math.abs(value))

    public override fun roundToIfWithinEpsilon(
        to: DataSize,
        epsilon: Double,
    ): DataSize =
        if (this.value in (to.value - epsilon)..(to.value + epsilon)) {
            to
        } else {
            this
        }

    public fun max(
        a: DataSize,
        b: DataSize,
    ): DataSize = if (a.value > b.value) a else b

    public fun min(
        a: DataSize,
        b: DataSize,
    ): DataSize = if (a.value < b.value) a else b

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Unit Specific Operations
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public operator fun div(timeDelta: TimeDelta): DataRate = DataRate.ofKBps(this.toKiB() / timeDelta.toSec())

    public operator fun div(duration: Duration): DataRate = this / duration.toTimeDelta()

    public operator fun div(dataRate: DataRate): TimeDelta = TimeDelta.ofSec(this.toKb() / dataRate.toKbps())

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Companion
    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public companion object : UnitId<DataSize> {
        @JvmStatic override val zero: DataSize = DataSize(.0)

        @JvmStatic override val max: DataSize = DataSize(Double.MAX_VALUE)

        @JvmStatic override val min: DataSize = DataSize(Double.MIN_VALUE)

        public operator fun Number.times(unit: DataSize): DataSize = unit * this

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

        // //////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Serializer
        // //////////////////////////////////////////////////////////////////////////////////////////////////////////////

        /**
         * Serializer for [DataSize] value class. It needs to be a compile
         * time constant to be used as serializer automatically,
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
