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

package org.opendc.simulator.compute.power

// TODO: couple this correctly
public enum class PowerModelEnum {
    Constant,
    Sqrt,
    Linear,
    Square,
    Cubic,
    MSE,
    Asymptotic,
}

public fun getPowerModel(
    modelType: String,
    power: Double,
    maxPower: Double,
    idlePower: Double,
    calibrationFactor: Double = 1.0,
    asymUtil: Double = 0.0,
    dvfs: Boolean = true,
): PowerModel {
    return when (modelType) {
        "constant" -> PowerModels.constant(power)
        "sqrt" -> PowerModels.sqrt(maxPower, idlePower)
        "linear" -> PowerModels.linear(maxPower, idlePower)
        "square" -> PowerModels.square(maxPower, idlePower)
        "cubic" -> PowerModels.cubic(maxPower, idlePower)
        "mse" -> PowerModels.mse(maxPower, idlePower, calibrationFactor)
        "asymptotic" -> PowerModels.asymptotic(maxPower, idlePower, asymUtil, dvfs)
        else -> throw IllegalArgumentException("Unknown power modelType $modelType")
    }
}

public fun getPowerModel(modelType: String): PowerModel {
    return when (modelType) {
        "constant" -> PowerModels.constant(200.0)
        "sqrt" -> PowerModels.sqrt(350.0, 200.0)
        "linear" -> PowerModels.linear(350.0, 200.0)
        "square" -> PowerModels.square(350.0, 200.0)
        "cubic" -> PowerModels.cubic(350.0, 200.0)
        "mse" -> PowerModels.mse(350.0, 200.0, 1.0)
        "asymptotic" -> PowerModels.asymptotic(350.0, 200.0, 0.0, true)
        else -> throw IllegalArgumentException("Unknown power modelType $modelType")
    }
}
