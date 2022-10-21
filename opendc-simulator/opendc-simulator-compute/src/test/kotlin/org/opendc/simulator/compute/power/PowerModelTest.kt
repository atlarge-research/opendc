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

package org.opendc.simulator.compute.power

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.math.pow

internal class PowerModelTest {
    private val epsilon = 10.0.pow(-3)
    private val cpuUtil = 0.9

    @ParameterizedTest
    @MethodSource("MachinePowerModelArgs")
    fun `compute power consumption given CPU loads`(
        powerModel: CpuPowerModel,
        expectedPowerConsumption: Double
    ) {
        val computedPowerConsumption = powerModel.computePower(cpuUtil)
        assertEquals(expectedPowerConsumption, computedPowerConsumption, epsilon)
    }

    @ParameterizedTest
    @MethodSource("MachinePowerModelArgs")
    fun `ignore idle power when computing power consumptions`(
        powerModel: CpuPowerModel,
        expectedPowerConsumption: Double
    ) {
        val zeroPowerModel = CpuPowerModels.zeroIdle(powerModel)

        assertAll(
            { assertEquals(expectedPowerConsumption, zeroPowerModel.computePower(cpuUtil), epsilon) },
            { assertEquals(0.0, zeroPowerModel.computePower(0.0)) }
        )
    }

    @Test
    fun `compute power draw by the SPEC benchmark model`() {
        val powerModel = CpuPowerModels.interpolate(
            58.4, 98.0, 109.0, 118.0, 128.0, 140.0, 153.0, 170.0, 189.0, 205.0, 222.0
        )

        assertAll(
            { assertEquals(58.4, powerModel.computePower(0.0)) },
            { assertEquals(58.4 + (98 - 58.4) / 5, powerModel.computePower(0.02)) },
            { assertEquals(98.0, powerModel.computePower(0.1)) },
            { assertEquals(140.0, powerModel.computePower(0.5)) },
            { assertEquals(189.0, powerModel.computePower(0.8)) },
            { assertEquals(189.0 + 0.7 * 10 * (205 - 189) / 10, powerModel.computePower(0.87)) },
            { assertEquals(205.0, powerModel.computePower(0.9)) },
            { assertEquals(222.0, powerModel.computePower(1.0)) }
        )
    }

    @Suppress("unused")
    private companion object {
        @JvmStatic
        fun MachinePowerModelArgs(): Stream<Arguments> = Stream.of(
            Arguments.of(CpuPowerModels.constant(0.0), 0.0),
            Arguments.of(CpuPowerModels.linear(350.0, 200.0), 335.0),
            Arguments.of(CpuPowerModels.square(350.0, 200.0), 321.5),
            Arguments.of(CpuPowerModels.cubic(350.0, 200.0), 309.35),
            Arguments.of(CpuPowerModels.sqrt(350.0, 200.0), 342.302),
            Arguments.of(CpuPowerModels.mse(350.0, 200.0, 1.4), 340.571),
            Arguments.of(CpuPowerModels.asymptotic(350.0, 200.0, 0.3, false), 338.765),
            Arguments.of(CpuPowerModels.asymptotic(350.0, 200.0, 0.3, true), 323.072)
        )
    }
}
