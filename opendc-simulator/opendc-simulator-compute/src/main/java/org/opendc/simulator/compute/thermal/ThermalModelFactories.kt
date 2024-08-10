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

// FIXME: This currently only works for RC models, we need to generalize this to support more models.

/**
 * A factory for creating thermal models.
 * @param modelType The type of the thermal model to create.
 * @param rHS The thermal resistance between the heat source and the heat sink.
 * @param rCase The thermal resistance between the heat sink and the ambient.
 * @param minLeakageCurrent The minimum leakage current of the heat source.
 * @param maxLeakageCurrent The maximum leakage current of the heat source.
 * @param supplyVoltage The supply voltage of the heat source.
 * @param ambientTemperature The ambient temperature of the heat source.
 * @return The thermal model.
 */
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

        else -> throw IllegalArgumentException("Unknown thermal modelType $modelType")
    }
}

/**
 * The default factory for creating RC thermal model using the values from an Intel Xeon Platinum 8160.
 */
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

        else -> throw IllegalArgumentException("Unknown thermal modelType $modelType")
    }
}
