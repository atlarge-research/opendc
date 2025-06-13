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

package org.opendc.simulator.compute.cpu

// TODO: couple this correctly
public enum class CPUPowerModelEnum {
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
): CpuPowerModel {
    return when (modelType) {
        "constant" -> CpuPowerModels.constant(power)
        "sqrt" -> CpuPowerModels.sqrt(maxPower, idlePower)
        "linear" -> CpuPowerModels.linear(maxPower, idlePower)
        "square" -> CpuPowerModels.square(maxPower, idlePower)
        "cubic" -> CpuPowerModels.cubic(maxPower, idlePower)
        "mse" -> CpuPowerModels.mse(maxPower, idlePower, calibrationFactor)
        "asymptotic" -> CpuPowerModels.asymptotic(maxPower, idlePower, asymUtil, dvfs)

        else -> throw IllegalArgumentException("Unknown power modelType $modelType")
    }
}

public fun getPowerModel(modelType: String): CpuPowerModel {
    return when (modelType) {
        "constant" -> CpuPowerModels.constant(200.0)
        "sqrt" -> CpuPowerModels.sqrt(350.0, 200.0)
        "linear" -> CpuPowerModels.linear(350.0, 200.0)
        "square" -> CpuPowerModels.square(350.0, 200.0)
        "cubic" -> CpuPowerModels.cubic(350.0, 200.0)
        "mse" -> CpuPowerModels.mse(350.0, 200.0, 1.0)
        "asymptotic" -> CpuPowerModels.asymptotic(350.0, 200.0, 0.0, true)

        else -> throw IllegalArgumentException("Unknown power modelType $modelType")
    }
}
