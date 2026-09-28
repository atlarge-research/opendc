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

package org.opendc.simulator.failure.models

/**
 * A definition of a Failure
 *
 * @property failureInterval The time between the end of the previous failure and the start of this one in ms, at least 0
 * @property failureDuration The Duration of the failure in ms, greater than 0
 * @property failureIntensity The ratio of hosts affected by the failure, in the range (0.0, 1.0]
 * @constructor Create empty Failure
 * @throws IllegalArgumentException if one of the properties is out of range
 */
public data class Failure(
    val failureInterval: Long,
    val failureDuration: Long,
    val failureIntensity: Double,
) {
    init {
        require(failureInterval >= 0) { "A failure cannot start at a negative time, but its interval was $failureInterval ms" }
        // A positive duration ensures every failure advances the simulation clock, even when its interval is 0
        require(failureDuration > 0) { "A failure must have a duration greater than 0, but its duration was $failureDuration ms" }
        require(failureIntensity > 0.0 && failureIntensity <= 1.0) {
            "The intensity of a failure has to be in the range (0.0, 1.0], but it was $failureIntensity"
        }
    }
}
