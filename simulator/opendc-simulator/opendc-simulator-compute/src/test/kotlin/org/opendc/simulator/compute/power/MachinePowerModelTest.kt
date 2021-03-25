package org.opendc.simulator.compute.power

import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.math.pow

@OptIn(ExperimentalCoroutinesApi::class)
internal class MachinePowerModelTest {
    private val epsilon = 10.0.pow(-3)
    private val cpuUtil = 0.9

    @ParameterizedTest
    @MethodSource("MachinePowerModelArgs")
    fun `compute power consumption given CPU loads`(
        powerModel: MachinePowerModel,
        expectedPowerConsumption: Double
    ) {
        val computedPowerConsumption = powerModel.computeCpuPower(cpuUtil)
        assertEquals(expectedPowerConsumption, computedPowerConsumption, epsilon)
    }

    @ParameterizedTest
    @MethodSource("MachinePowerModelArgs")
    fun `ignore idle power when computing power consumptions`(
        powerModel: MachinePowerModel,
        expectedPowerConsumption: Double
    ) {
        val zeroPowerModel = ZeroIdlePowerDecorator(powerModel)
        val computedPowerConsumption = zeroPowerModel.computeCpuPower(0.0)
        assertEquals(0.0, computedPowerConsumption)
    }

    @Suppress("unused")
    private companion object {
        @JvmStatic
        fun MachinePowerModelArgs(): Stream<Arguments> = Stream.of(
            Arguments.of(ConstantPowerModel(0.0), 0.0),
            Arguments.of(LinearPowerModel(350.0, 200 / 350.0), 335.0),
            Arguments.of(SquarePowerModel(350.0, 200 / 350.0), 321.5),
            Arguments.of(CubicPowerModel(350.0, 200 / 350.0), 309.35),
            Arguments.of(SqrtPowerModel(350.0, 200 / 350.0), 342.302),
        )
    }
}
