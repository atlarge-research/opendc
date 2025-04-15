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

package org.opendc.simulator.compute.gpu

// TODO: couple this correctly
public enum class GPUPowerModel {
    Constant,
    Sqrt,
    Linear,
    Square,
    Cubic,
}

public fun getPowerModel(
    modelType: String,
    power: Double,
    maxPower: Double,
    idlePower: Double,
): GpuPowerModel {
    return when (modelType) {
        "constant" -> GpuPowerModels.constant(power)
        "sqrt" -> GpuPowerModels.sqrt(maxPower, idlePower)
        "linear" -> GpuPowerModels.linear(maxPower, idlePower)
        "square" -> GpuPowerModels.square(maxPower, idlePower)
        "cubic" -> GpuPowerModels.cubic(maxPower, idlePower)

        else -> throw IllegalArgumentException("Unknown power modelType $modelType")
    }
}

public fun getPowerModel(modelType: String): GpuPowerModel {
    return when (modelType) {
        "constant" -> GpuPowerModels.constant(200.0)
        "sqrt" -> GpuPowerModels.sqrt(350.0, 200.0)
        "linear" -> GpuPowerModels.linear(350.0, 200.0)
        "square" -> GpuPowerModels.square(350.0, 200.0)
        "cubic" -> GpuPowerModels.cubic(350.0, 200.0)

        else -> throw IllegalArgumentException("Unknown power modelType $modelType")
    }
}
