/*
 * MIT License
 *
 * Copyright (c) 2018 atlarge-research
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

package com.atlarge.odcsim

/**
 * An instantaneous point on the time-line, used to record message time-stamps in a simulation.
 */
typealias Instant = Double

/**
 * A time interval which represents the amount of elapsed time between two messages.
 */
typealias Duration = Double

/**
 * Convert this [Int] into an [Instant].
 */
fun Int.toInstant(): Instant = toDouble()

/**
 * Convert this [Int] into a [Duration].
 */
fun Int.toDuration(): Duration = toDouble()

/**
 * Convert this [Long] into an [Instant].
 */
fun Long.toInstant(): Instant = toDouble()

/**
 * Convert this [Long] into a [Duration].
 */
fun Long.toDuration(): Duration = toDouble()

/**
 * Convert this [Float] into an [Instant].
 */
fun Float.toInstant(): Instant = toDouble()

/**
 * Convert this [Float] into a [Duration].
 */
fun Float.toDuration(): Duration = toDouble()

/**
 * Convert this [Double] into an [Instant].
 */
fun Double.toInstant(): Instant = toDouble()

/**
 * Convert this [Double] into a [Duration].
 */
fun Double.toDuration(): Duration = toDouble()
