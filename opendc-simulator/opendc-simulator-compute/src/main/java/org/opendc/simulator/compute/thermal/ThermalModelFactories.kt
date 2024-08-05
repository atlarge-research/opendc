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

package org.opendc.simulator.compute.thermal

public fun getThermalModel(
    modelType: String,
    rHS: Double,
    rCase: Double,
    minLeakageCurrent: Double,
    maxLeakageCurrent: Double,
    supplyVoltage: Double,
    ambientTemperature: Double,
): ThermalModel {
    return when (modelType) {
        "rcmodel" ->
            ThermalModels.rcmodel(
                rHS,
                rCase,
                minLeakageCurrent,
                maxLeakageCurrent,
                supplyVoltage,
                ambientTemperature,
            )

        else -> throw IllegalArgumentException("Unknown power modelType $modelType")
    }
}

public fun getThermalModel(modelType: String): ThermalModel {
    return when (modelType) {
        "rcmodel" ->
            ThermalModels.rcmodel(
                0.298,
                0.00061,
                0.00035,
                0.0041,
                1.8,
                22.0,
            )
        "manufacturerModel" ->
            ThermalModels.manufacturerModel(0.278, 47.0)

        else -> throw IllegalArgumentException("Unknown power modelType $modelType")
    }
}
