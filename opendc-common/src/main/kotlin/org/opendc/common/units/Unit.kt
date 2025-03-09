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

import org.opendc.common.annotations.InternalUse
import org.opendc.common.units.TimeDelta.Companion.toTimeDelta
import org.opendc.common.utils.DFLT_MIN_EPS
import org.opendc.common.utils.adaptiveEps
import org.opendc.common.utils.approx
import org.opendc.common.utils.approxLarger
import org.opendc.common.utils.approxLargerOrEq
import org.opendc.common.utils.approxSmaller
import org.opendc.common.utils.approxSmallerOrEq
import java.time.Duration
import kotlin.experimental.ExperimentalTypeInference

/**
 * Value classes can extend this interface to represent
 * unit of measure with much higher type safety than [Double] (*If used from kotlin*)
 * and approximately same performances.
 * ```kotlin
 * // e.g.
 * @JvmInline value class DataRate(override val value) : Unit<DataRate> { }
 * ```
 * This interface provides most of the utility functions and
 * mathematical operations that are available to [Double] (including threshold comparison methods),
 * but applicable to [T] (also with scalar multiplication and division),
 * and operations between different unit of measures (e.g., DataRate * TimeDelta = DataSize, and many others).
 *
 * ```
 * // e.g. sum of data-rates
 * val a: DataRate = DataRate.ofMibps(100)
 * val b: DataRate = DataRate.ofGibps(1)
 * val c: DataRate = a + b
 * c.fmt("%.3f") // "1.097 Gibps"
 *
 * // e.g. data-rate times scalar
 * val e: DataRate = a * 2
 * e.fmt() // "200 Mibps"
 *
 * // e.g. threshold comparison
 * if (e approx a) { ... }
 *
 * // e.g. operations between different unit of measures
 * val a: DataRate = DataRate.ofMBps(1)
 * val b: TimeDelta = TimeDelta.ofSec(3)
 * val c: DataSize = a * b
 * c.fmt() // "3MB"
 * ```
 * &nbsp;
 * ###### Java interoperability
 * Functions that concern inline classes are not callable from java by default (at least for now).
 * Hence, the JvmName annotation is necessary for java interoperability.Only methods that allow java
 * to interact with kotlin code concerning inline classes should be made accessible to java.**
 * Java will never be able to invoke instance methods, only static ones.
 *
 * Java sees value classes as the standard data type they represent (in this case, double).
 * Meaning there is no type safety from java, nevertheless, functions can be invoked
 * to provide methods with the correct unit value (and for improved understandability).
 *
 * ```kotlin
 * // kotlin
 * @JvmStatic @JvmName("function")
 * fun function(time: TimeDelta) {  }
 * ```
 * ```java
 * // java
 * double time = TimeDelta.ofHours(2);
 * function(time)
 * // or
 * function(TimeDelta.ofHours(2))
 * ```
 *
 * @param[T] the unit of measure that is represented (e.g. [DataRate])
 */
public sealed interface Unit<T : Unit<T>> : Comparable<T> {
    /**
     * The actual value of this unit of measure used for computation and comparisons.
     *
     * What magnitude this value represents (e.g., Kbps, Mbps etc.) is up to the interface implementation,
     * and it does not interfere with the operations; hence this property should be reserved for internal use.
     */
    @InternalUse
    @NonInlinableUnit
    public val value: Double

    /**
     * If `this` is -0.0 it is converted to +0.0.
     */
    @NonInlinableUnit
    public fun ifNeg0ThenPos0(): T

    /**
     * @return the sum with [other] as [T].
     */
    @NonInlinableUnit
    public operator fun plus(other: T): T

    /**
     * @return the subtraction of [other] from *this* as [T].
     */
    @NonInlinableUnit
    public operator fun minus(other: T): T

    /**
     * @return *this* divided by scalar [scalar] as [T].
     */
    @NonInlinableUnit
    public operator fun div(scalar: Number): T

    /**
     * @return *this* divided by [other] as [Double].
     */
    @NonInlinableUnit
    public operator fun div(other: T): Percentage

    /**
     * @return *this* multiplied by scalar [scalar] as [T].
     */
    @NonInlinableUnit
    public operator fun times(scalar: Number): T

    /**
     * @return *this* multiplied by percentage [Percentage] as [T].
     */
    @NonInlinableUnit
    public operator fun times(percentage: Percentage): T

    /**
     * @return *this* negated.
     */
    @NonInlinableUnit
    public operator fun unaryMinus(): T

    /**
     * @return `true` if *this* is equal to 0 (using `==` operator).
     */
    @NonInlinableUnit
    public fun isZero(): Boolean

    /**
     * @return `true` if *this* is approximately equal to 0.
     * @see[Double.approx]
     */
    @NonInlinableUnit
    public fun approxZero(epsilon: Double = DFLT_MIN_EPS): Boolean

    /**
     * @see[Double.approx]
     */
    @NonInlinableUnit
    public fun approx(
        other: T,
        minEpsilon: Double = DFLT_MIN_EPS,
        epsilon: Double = adaptiveEps(this.value, other.value, minEpsilon),
    ): Boolean

    /**
     * @see[Double.approx]
     */
    @NonInlinableUnit
    public infix fun approx(other: T): Boolean

    /**
     * @see[Double.approxLarger]
     */
    @NonInlinableUnit
    public fun approxLarger(
        other: T,
        minEpsilon: Double = DFLT_MIN_EPS,
        epsilon: Double = adaptiveEps(this.value, other.value, minEpsilon),
    ): Boolean

    /**
     * @see[Double.approxLarger]
     */
    @NonInlinableUnit
    public infix fun approxLarger(other: T): Boolean

    /**
     * @see[Double.approxLargerOrEq]
     */
    @NonInlinableUnit
    public fun approxLargerOrEq(
        other: T,
        minEpsilon: Double = DFLT_MIN_EPS,
        epsilon: Double = adaptiveEps(this.value, other.value, minEpsilon),
    ): Boolean

    /**
     * @see[Double.approxLargerOrEq]
     */
    @NonInlinableUnit
    public infix fun approxLargerOrEq(other: T): Boolean

    /**
     * @see[Double.approxSmaller]
     */
    @NonInlinableUnit
    public fun approxSmaller(
        other: T,
        minEpsilon: Double = DFLT_MIN_EPS,
        epsilon: Double = adaptiveEps(this.value, other.value, minEpsilon),
    ): Boolean

    /**
     * @see[Double.approxSmaller]
     */
    @NonInlinableUnit
    public infix fun approxSmaller(other: T): Boolean

    /**
     * @see[Double.approxSmallerOrEq]
     */
    @NonInlinableUnit
    public fun approxSmallerOrEq(
        other: T,
        minEpsilon: Double = DFLT_MIN_EPS,
        epsilon: Double = adaptiveEps(this.value, other.value, minEpsilon),
    ): Boolean

    /**
     * @see[Double.approxSmallerOrEq]
     */
    @NonInlinableUnit
    public infix fun approxSmallerOrEq(other: T): Boolean

    /**
     * @return the max value between *this* and [other].
     */
    @NonInlinableUnit
    public infix fun max(other: T): T

    /**
     * @return the minimum value between *this* and [other].
     */
    @NonInlinableUnit
    public infix fun min(other: T): T

    /**
     * @return the absolute value of *this*.
     */
    @NonInlinableUnit
    public fun abs(): T

    /**
     * @return *this* approximated to [to] if within `0 - epsilon` and `0 + epsilon`.
     */
    @NonInlinableUnit
    public fun roundToIfWithinEpsilon(
        to: T,
        epsilon: Double = DFLT_MIN_EPS,
    ): T

    /**
     * Returns the formatted string representation of the unit of measure (e.g. "1.2 Gbps")
     * with the formatter [fmt] applied to the value part of the resulting string.
     *
     * ```kotlin
     * val dr = DataRate.ofGbps(1.234567)
     * dr.fmtValue() // "1.234567 Gbps"
     * dr.fmtValue("%.2f") // "1.23 Gbps"
     * ```
     */
    public fun fmtValue(fmt: String = "%f"): String

    public companion object {
        /**
         * @return [unit] multiplied by scalar [this].
         */
        @NonInlinableUnit
        public operator fun <T : Unit<T>> Number.times(unit: T): T = unit * this

        /**
         * @return minimum value between [units].
         */
        @NonInlinableUnit
        public inline fun <reified T : Unit<T>> minOf(vararg units: T): T = units.minBy { it.value }

        /**
         * @return maximum value between [units].
         */
        @NonInlinableUnit
        public inline fun <reified T : Unit<T>> maxOf(vararg units: T): T = units.maxBy { it.value }

        public operator fun Duration.times(dataRate: DataRate): DataSize = toTimeDelta() * dataRate

        public operator fun Duration.times(power: Power): Energy = toTimeDelta() * power

        public operator fun Number.div(timeDelta: TimeDelta): Frequency = Frequency.ofHz(this.toDouble() / timeDelta.toSec())

        public operator fun Number.div(duration: Duration): Frequency = this / duration.toTimeDelta()

        // Defined here so that they can overload the same method name, instead of having a different name forEach unit.
        // You cannot overload `sumOf` and using that name results in not
        // being able to use the overloads for unit and for number in the same file.

        // A reified version that does not need overloads can also be defined,
        // with a switch statement on the reified unit type for the base value.
        // Then, if a unit is not included in the switch, a runtime error occurs, not compile time.

        @OptIn(ExperimentalTypeInference::class)
        @OverloadResolutionByLambdaReturnType
        @JvmName("sumOfDataRate")
        public inline fun <T> Iterable<T>.sumOfUnit(selector: (T) -> DataRate): DataRate {
            var sum: DataRate = DataRate.zero
            forEach { sum += selector(it) }
            return sum
        }

        @OptIn(ExperimentalTypeInference::class)
        @OverloadResolutionByLambdaReturnType
        @JvmName("sumOfDataSize")
        public inline fun <T> Iterable<T>.sumOfUnit(selector: (T) -> DataSize): DataSize {
            var sum: DataSize = DataSize.zero
            forEach { sum += selector(it) }
            return sum
        }

        @OptIn(ExperimentalTypeInference::class)
        @OverloadResolutionByLambdaReturnType
        @JvmName("sumOfEnergy")
        public inline fun <T> Iterable<T>.sumOfUnit(selector: (T) -> Energy): Energy {
            var sum: Energy = Energy.zero
            forEach { sum += selector(it) }
            return sum
        }

        @OptIn(ExperimentalTypeInference::class)
        @OverloadResolutionByLambdaReturnType
        @JvmName("sumOfPower")
        public inline fun <T> Iterable<T>.sumOfUnit(selector: (T) -> Power): Power {
            var sum: Power = Power.zero
            forEach { sum += selector(it) }
            return sum
        }

        @OptIn(ExperimentalTypeInference::class)
        @OverloadResolutionByLambdaReturnType
        @JvmName("sumOfTime")
        public inline fun <T> Iterable<T>.sumOfUnit(selector: (T) -> TimeDelta): TimeDelta {
            var sum: TimeDelta = TimeDelta.zero
            forEach { sum += selector(it) }
            return sum
        }

        @OptIn(ExperimentalTypeInference::class)
        @OverloadResolutionByLambdaReturnType
        @JvmName("sumOfFrequency")
        public inline fun <T> Iterable<T>.sumOfUnit(selector: (T) -> Frequency): Frequency {
            var sum: Frequency = Frequency.zero
            forEach { sum += selector(it) }
            return sum
        }

        @OptIn(ExperimentalTypeInference::class)
        @OverloadResolutionByLambdaReturnType
        @JvmName("sumOfPercentage")
        public inline fun <T> Iterable<T>.sumOfUnit(selector: (T) -> Percentage): Percentage {
            var sum: Percentage = Percentage.zero
            forEach { sum += selector(it) }
            return sum
        }

        @OptIn(ExperimentalTypeInference::class)
        @OverloadResolutionByLambdaReturnType
        @JvmName("averageOfDataRateOrNull")
        public inline fun <T> Iterable<T>.averageOfUnitOrNull(selector: (T) -> DataRate): DataRate? {
            if (!iterator().hasNext()) return null
            var sum: DataRate = DataRate.zero
            var count = 0
            forEach {
                sum += selector(it)
                count++
            }
            return sum / count
        }

        @OptIn(ExperimentalTypeInference::class)
        @OverloadResolutionByLambdaReturnType
        @JvmName("averageOfDataSizeOrNull")
        public inline fun <T> Iterable<T>.averageOfUnitOrNull(selector: (T) -> DataSize): DataSize? {
            if (!iterator().hasNext()) return null
            var sum: DataSize = DataSize.zero
            var count = 0
            forEach {
                sum += selector(it)
                count++
            }
            return sum / count
        }

        @OptIn(ExperimentalTypeInference::class)
        @OverloadResolutionByLambdaReturnType
        @JvmName("averageOfEnergyOrNull")
        public inline fun <T> Iterable<T>.averageOfUnitOrNull(selector: (T) -> Energy): Energy? {
            if (!iterator().hasNext()) return null
            var sum: Energy = Energy.zero
            var count = 0
            forEach {
                sum += selector(it)
                count++
            }
            return sum / count
        }

        @OptIn(ExperimentalTypeInference::class)
        @OverloadResolutionByLambdaReturnType
        @JvmName("averageOfPowerOrNull")
        public inline fun <T> Iterable<T>.averageOfUnitOrNull(selector: (T) -> Power): Power? {
            if (!iterator().hasNext()) return null
            var sum: Power = Power.zero
            var count = 0
            forEach {
                sum += selector(it)
                count++
            }
            return sum / count
        }

        @OptIn(ExperimentalTypeInference::class)
        @OverloadResolutionByLambdaReturnType
        @JvmName("averageOfTimeOrNull")
        public inline fun <T> Iterable<T>.averageOfUnitOrNull(selector: (T) -> TimeDelta): TimeDelta? {
            if (!iterator().hasNext()) return null
            var sum: TimeDelta = TimeDelta.zero
            var count = 0
            forEach {
                sum += selector(it)
                count++
            }
            return sum / count
        }

        @OptIn(ExperimentalTypeInference::class)
        @OverloadResolutionByLambdaReturnType
        @JvmName("averageOfFrequencyOrNull")
        public inline fun <T> Iterable<T>.averageOfUnitOrNull(selector: (T) -> Frequency): Frequency? {
            if (!iterator().hasNext()) return null
            var sum: Frequency = Frequency.zero
            var count = 0
            forEach {
                sum += selector(it)
                count++
            }
            return sum / count
        }

        @OptIn(ExperimentalTypeInference::class)
        @OverloadResolutionByLambdaReturnType
        @JvmName("averageOfPercentageOrNull")
        public inline fun <T> Iterable<T>.averageOfUnitOrNull(selector: (T) -> Percentage): Percentage? {
            if (!iterator().hasNext()) return null
            var sum: Percentage = Percentage.zero
            var count = 0
            forEach {
                sum += selector(it)
                count++
            }
            return sum / count
        }
    }
}

@RequiresOptIn(
    message =
        "Unit value class cannot be JVM inlined if this symbol is used " +
            "(and if value class is used as generic type, but that holds for `double` as well)",
    level = RequiresOptIn.Level.WARNING,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR)
public annotation class NonInlinableUnit

@RequiresOptIn(
    message =
        "This operation is not intended for this unit, but it needs to be define. " +
            "Invoking this method will result in an exception. ",
    level = RequiresOptIn.Level.WARNING,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR)
public annotation class UnintendedOperation

public class UnitOperationException(override val message: String? = null) : Exception()

public interface UnitId<T : Unit<T>> {
    public val zero: T
    public val max: T
    public val min: T
}
