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
        powerModel: PowerModel,
        expectedPowerConsumption: Double
    ) {
        val computedPowerConsumption = powerModel.computePower(cpuUtil)
        assertEquals(expectedPowerConsumption, computedPowerConsumption, epsilon)
    }

    @ParameterizedTest
    @MethodSource("MachinePowerModelArgs")
    fun `ignore idle power when computing power consumptions`(
        powerModel: PowerModel,
        expectedPowerConsumption: Double
    ) {
        val zeroPowerModel = ZeroIdlePowerDecorator(powerModel)

        assertAll(
            { assertEquals(expectedPowerConsumption, zeroPowerModel.computePower(cpuUtil), epsilon) },
            { assertEquals(0.0, zeroPowerModel.computePower(0.0)) }
        )
    }

    @Test
    fun `compute power draw by the SPEC benchmark model`() {
        val powerModel = InterpolationPowerModel("IBMx3550M3_XeonX5675")

        assertAll(
            { assertEquals(58.4, powerModel.computePower(0.0)) },
            { assertEquals(58.4 + (98 - 58.4) / 5, powerModel.computePower(0.02)) },
            { assertEquals(98.0, powerModel.computePower(0.1)) },
            { assertEquals(140.0, powerModel.computePower(0.5)) },
            { assertEquals(189.0, powerModel.computePower(0.8)) },
            { assertEquals(189.0 + 0.7 * 10 * (205 - 189) / 10, powerModel.computePower(0.87)) },
            { assertEquals(205.0, powerModel.computePower(0.9)) },
            { assertEquals(222.0, powerModel.computePower(1.0)) },
        )
    }

    @Suppress("unused")
    private companion object {
        @JvmStatic
        fun MachinePowerModelArgs(): Stream<Arguments> = Stream.of(
            Arguments.of(ConstantPowerModel(0.0), 0.0),
            Arguments.of(LinearPowerModel(350.0, 200.0), 335.0),
            Arguments.of(SquarePowerModel(350.0, 200.0), 321.5),
            Arguments.of(CubicPowerModel(350.0, 200.0), 309.35),
            Arguments.of(SqrtPowerModel(350.0, 200.0), 342.302),
        )
    }
}
