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
import org.opendc.common.logger.logger
import org.opendc.common.utils.fmt
import org.opendc.common.utils.ifNeg0thenPos0
import kotlin.text.RegexOption.IGNORE_CASE

/**
 * Represents a percentage. This interface has 2 value classes implementations.
 *
 * Using the interface instead of its implementation will likely result in worse
 * performances compared to using the value-classes themselves,
 * since the jvm will allocate an object for the interface. Therefore, it is suggested
 * to use the interface as little as possible. Operations between the same implementation
 * ([BoundedPercentage] + [BoundedPercentage]) will result in the same return type.
 *
 * [BoundedPercentage]s are adjusted to remain in range 0-100%,
 * logging warning whenever an adjustment has been made.
 *
 * As all [Unit]s, offers the vast majority
 * of mathematical operations that one would perform on a simple [Double].
 */
@Serializable(with = Percentage.Companion.PercentageSerializer::class)
public sealed interface Percentage : Unit<Percentage> {
    override val value: Double

    /**
     * @return the value as a ratio (e.g. 50% -> 0.5)
     */
    public fun toRatio(): Double = value

    /**
     * @return the value as percentage (50.6% -> 50.6)
     */
    public fun toPercentageValue(): Double = value * 1e2

    /**
     * @return *this* percentage converted to [BoundedPercentage].
     */
    public fun toBoundedPercentage(): BoundedPercentage

    /**
     * @return *this* percentage converted to [UnboundedPercentage].
     */
    public fun toUnboundedPercentage(): UnboundedPercentage

    /**
     * ```kotlin
     * // e.g.
     * val perc: Percentage = Percentage.ofRatio(0.123456789)
     * perc.fmtValue("%.4f") // "12.3456%"
     * ```
     *
     * @see[Unit.fmtValue]
     */
    override fun fmtValue(fmt: String): String = "${toPercentageValue().fmt(fmt)}%"

    public companion object {
        @JvmStatic public val ZERO: Percentage = UnboundedPercentage(.0)

        @JvmStatic
        @JvmName("ofRatio")
        public fun ofRatio(ratio: Double): UnboundedPercentage = UnboundedPercentage(ratio)

        @JvmStatic
        @JvmName("ofRatioBounded")
        public fun ofRatioBounded(ratio: Double): BoundedPercentage = BoundedPercentage(ratio)

        @JvmStatic
        @JvmName("ofPercentage")
        public fun ofPercentage(percentage: Number): UnboundedPercentage = UnboundedPercentage(percentage.toDouble() / 100)

        @JvmStatic
        @JvmName("ofPercentageBounded")
        public fun ofPercentageBounded(percentage: Double): BoundedPercentage = BoundedPercentage(percentage / 100)

        /**
         * @return the percentage resulting from [this] / [other].
         */
        public infix fun Number.percentageOf(other: Number): UnboundedPercentage = UnboundedPercentage(this.toDouble() / other.toDouble())

        /**
         * @return the *bounded* percentage resulting from [this] / [other].
         */
        public infix fun Number.boundedPercentageOf(other: Number): BoundedPercentage =
            BoundedPercentage(this.toDouble() / other.toDouble())

        /**
         * @return the percentage resulting from [this] / [other], applicable on all [Unit]s of same type.
         */
        public infix fun <T : Unit<T>> T.percentageOf(other: T): UnboundedPercentage = UnboundedPercentage(this.value / other.value)

        /**
         * @return the *bounded* percentage resulting from [this] / [other], applicable on all [Unit]s of same type.
         */
        public infix fun <T : Unit<T>> T.boundedPercentageOf(other: T): BoundedPercentage = BoundedPercentage(this.value / other.value)

        private val PERCENTAGE = Regex("\\s*(?:percentage|Percentage|%)\\s*?")

        /**
         * Serializer for [Percentage] value class. It needs to be a compile
         * time constant in order to be used as serializer automatically,
         * hence `object :` instead of class instantiation.
         *
         * For implementation purposes it always deserialize an [UnboundedPercentage] as [Percentage].
         *
         * ```json
         * // e.g.
         * "percentage": 0.5 // 50% with warning
         * "percentage": "  30%   "
         * "percentage": "120%" // 120% (unbounded)
         * // etc.
         * ```
         */
        internal object PercentageSerializer : UnitSerializer<Percentage>(
            ifNumber = {
                LOG.warn(
                    "deserialization of number with no unit of measure, assuming it is a ratio." +
                        "Keep in mind that you can also specify the value as '${it.toDouble() * 100}%'",
                )
                ofRatio(it.toDouble())
            },
            serializerFun = { this.encodeString(it.toString()) },
            ifMatches("$NUM_GROUP$PERCENTAGE", IGNORE_CASE) { ofPercentage(json.decNumFromStr(groupValues[1])) },
        )
    }
}

/**
 * Bounded implementation of [Percentage], meaning the
 * percentage value is adjusted to always be in the range 0-100%,
 * logging a warning whenever an adjustment has been made.
 */
@JvmInline
public value class BoundedPercentage
    @InternalUse
    internal constructor(
        override val value: Double,
    ) : Percentage {
        override fun toBoundedPercentage(): BoundedPercentage = this

        override fun toUnboundedPercentage(): UnboundedPercentage = UnboundedPercentage(value)

        override fun new(value: Double): BoundedPercentage = BoundedPercentage(value.forceInRange().ifNeg0thenPos0())

        override fun toString(): String = fmtValue()

        /**
         * "Override" to return [BoundedPercentage] insteadof [Percentage].
         * @see[Unit.plus]
         */
        public infix operator fun plus(other: BoundedPercentage): BoundedPercentage = BoundedPercentage(this.value + other.value)

        /**
         * "Override" to return [BoundedPercentage] insteadof [Percentage].
         * @see[Unit.minus]
         */
        public infix operator fun minus(other: BoundedPercentage): BoundedPercentage = BoundedPercentage(this.value - other.value)

        /**
         * Override to return [BoundedPercentage] insteadof [Percentage].
         * @see[Unit.times]
         */
        override operator fun times(scalar: Number): BoundedPercentage = BoundedPercentage(this.value * scalar.toDouble())

        /**
         * Override to return [BoundedPercentage] insteadof [Percentage].
         * @see[Unit.div]
         */
        override operator fun div(scalar: Number): BoundedPercentage = BoundedPercentage(this.value / scalar.toDouble())

        private fun Double.forceInRange(
            from: Double = .0,
            to: Double = 1.0,
        ): Double =
            if (this < from) {
                LOG.warn("bounded percentage has been rounded up (from ${this * 1e2}% to ${from * 1e2}%")
                from
            } else if (this > to) {
                LOG.warn("bounded percentage has been rounded down (from ${this * 1e2}% to ${to * 1e2}%")
                to
            } else {
                this
            }

        public companion object {
            private val LOG by logger()
        }
    }

/**
 * Unbounded implementation of [Percentage], meaning the
 * percentage value is allowed to be outside the range 0-100%.
 */
@JvmInline
public value class UnboundedPercentage
    @InternalUse
    internal constructor(
        override val value: Double,
    ) : Percentage {
        override fun toBoundedPercentage(): BoundedPercentage = BoundedPercentage(value.ifNeg0thenPos0())

        override fun toUnboundedPercentage(): UnboundedPercentage = this

        @InternalUse
        override fun new(value: Double): UnboundedPercentage = UnboundedPercentage(value)

        override fun toString(): String = fmtValue()

        /**
         * "Override" to return [UnboundedPercentage] insteadof [Percentage].
         * @see[Unit.plus]
         */
        public infix operator fun plus(other: UnboundedPercentage): UnboundedPercentage = UnboundedPercentage(this.value + other.value)

        /**
         * "Override" to return [UnboundedPercentage] insteadof [Percentage].
         * @see[Unit.minus]
         */
        public infix operator fun minus(other: UnboundedPercentage): UnboundedPercentage = UnboundedPercentage(this.value - other.value)

        /**
         * Override to return [UnboundedPercentage] insteadof [Percentage].
         * @see[Unit.times]
         */
        override operator fun times(scalar: Number): UnboundedPercentage = UnboundedPercentage(this.value * scalar.toDouble())

        /**
         * Override to return [UnboundedPercentage] insteadof [Percentage].
         * @see[Unit.div]
         */
        override operator fun div(scalar: Number): UnboundedPercentage = UnboundedPercentage(this.value / scalar.toDouble())
    }
