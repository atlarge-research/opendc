/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.faas.simulator.delay

/**
 * Model parameters for the cold start times of serverless services.
 */
public enum class ColdStartModel {
    // Min and max memory values from [Peeking Behind The Curtains of Serverless Platforms][2018],
    // other values deduced from linear curve.
    LAMBDA {
        override fun coldStartParam(provisionedMemory: Int): Pair<Double, Double> {
            return when (provisionedMemory) {
                128 -> Pair(265.21, 354.43)
                256 -> Pair(261.46, 334.23)
                512 -> Pair(257.71, 314.03)
                1024 -> Pair(253.96, 293.83)
                1536 -> Pair(250.07, 273.63)
                2048 -> Pair(246.11, 253.43)
                else -> Pair(0.0, 1.0)
            }
        }
    },
    AZURE {
        // Azure by default uses 1.5gb memory to instantiate functions
        override fun coldStartParam(provisionedMemory: Int): Pair<Double, Double> {
            return Pair(242.66, 340.67)
        }
    },

    GOOGLE {
        override fun coldStartParam(provisionedMemory: Int): Pair<Double, Double> {
            return when (provisionedMemory) {
                128 -> Pair(493.04, 345.8)
                256 -> Pair(416.59, 301.5)
                512 -> Pair(340.14, 257.2)
                1024 -> Pair(263.69, 212.9)
                1536 -> Pair(187.24, 168.6)
                2048 -> Pair(110.77, 124.3)
                else -> Pair(0.0, 1.0)
            }
        }
    };

    /**
     * Obtain the stochastic parameters for the cold start models.
     */
    public abstract fun coldStartParam(provisionedMemory: Int): Pair<Double, Double>
}
