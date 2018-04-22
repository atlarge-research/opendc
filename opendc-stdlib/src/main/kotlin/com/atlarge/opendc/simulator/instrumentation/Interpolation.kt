/*
 * MIT License
 *
 * Copyright (c) 2017 atlarge-research
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

package com.atlarge.opendc.simulator.instrumentation

import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consume
import kotlinx.coroutines.experimental.channels.produce
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Interpolate [n] amount of elements between every two occurrences of elements passing through the channel.
 *
 * The operation is _intermediate_ and _stateless_.
 * This function [consumes][consume] all elements of the original [ReceiveChannel].
 *
 * @param context The context of the coroutine.
 * @param n The amount of elements to interpolate between the actual elements in the channel.
 * @param interpolator A function to interpolate between the two element occurrences.
 */
fun <E> ReceiveChannel<E>.interpolate(n: Int, context: CoroutineContext = Unconfined,
                                      interpolator: (Double, E, E) -> E): ReceiveChannel<E> =
    produce(context) {
        consume {
            val iterator = iterator()

            if (!iterator.hasNext())
                return@produce

            var a = iterator.next()
            send(a)

            while (iterator.hasNext()) {
                val b = iterator.next()
                for (i in 1..n) {
                    send(interpolator(i.toDouble() / (n + 1), a, b))
                }
                send(b)
                a = b
            }
        }
    }

/**
 * Perform a linear interpolation on the given double values.
 *
 * @param a The start value
 * @param b The end value
 * @param f The amount to interpolate which represents the position between the two values as a percentage in [0, 1].
 * @return The interpolated double result between the double values.
 */
fun lerp(a: Double, b: Double, f: Double): Double = a + f * (b - a)

/**
 * Perform a linear interpolation on the given float values.
 *
 * @param a The start value
 * @param b The end value
 * @param f The amount to interpolate which represents the position between the two values as a percentage in [0, 1].
 * @return The interpolated float result between the float values.
 */
fun lerp(a: Float, b: Float, f: Float): Float = a + f * (b - a)

/**
 * Perform a linear interpolation on the given integer values.
 *
 * @param a The start value
 * @param b The end value
 * @param f The amount to interpolate which represents the position between the two values as a percentage in [0, 1].
 * @return The interpolated integer result between the integer values.
 */
fun lerp(a: Int, b: Int, f: Float): Int = lerp(a.toFloat(), b.toFloat(), f).toInt()

/**
 * Perform a linear interpolation on the given integer values.
 *
 * @param a The start value
 * @param b The end value
 * @param f The amount to interpolate which represents the position between the two values as a percentage in [0, 1].
 * @return The interpolated integer result between the integer values.
 */
fun lerp(a: Int, b: Int, f: Double): Int = lerp(a.toDouble(), b.toDouble(), f).toInt()

/**
 * Perform a linear interpolation on the given long values.
 *
 * @param a The start value
 * @param b The end value
 * @param f The amount to interpolate which represents the position between the two values as a percentage in [0, 1].
 * @return The interpolated long result between the long values.
 */
fun lerp(a: Long, b: Long, f: Double): Long = lerp(a.toDouble(), b.toDouble(), f).toLong()

/**
 * Perform a linear interpolation on the given long values.
 *
 * @param a The start value
 * @param b The end value
 * @param f The amount to interpolate which represents the position between the two values as a percentage in [0, 1].
 * @return The interpolated long result between the long values.
 */
fun lerp(a: Long, b: Long, f: Float): Long = lerp(a.toFloat(), b.toFloat(), f).toLong()
