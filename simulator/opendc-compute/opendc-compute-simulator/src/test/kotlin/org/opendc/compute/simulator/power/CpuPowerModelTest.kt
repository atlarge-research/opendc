package org.opendc.compute.simulator.power

import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.opendc.compute.simulator.power.api.CpuPowerModel
import org.opendc.compute.simulator.power.models.*
import org.opendc.simulator.compute.SimBareMetalMachine
import java.util.stream.Stream
import kotlin.math.pow

@OptIn(ExperimentalCoroutinesApi::class)
internal class CpuPowerModelTest {
    private val epsilon = 10.0.pow(-3)
    private val cpuUtil = 0.9

    @ParameterizedTest
    @MethodSource("cpuPowerModelArgs")
    fun `compute power consumption given CPU loads`(
        powerModel: CpuPowerModel,
        expectedPowerConsumption: Double
    ) {
        val computedPowerConsumption = powerModel.computeCpuPower(cpuUtil)
        assertEquals(expectedPowerConsumption, computedPowerConsumption, epsilon)
    }

    @ParameterizedTest
    @MethodSource("cpuPowerModelArgs")
    fun `ignore idle power when computing power consumptions`(
        powerModel: CpuPowerModel,
        expectedPowerConsumption: Double
    ) {
        val zeroPowerModel = ZeroIdlePowerDecorator(powerModel)
        val computedPowerConsumption = zeroPowerModel.computeCpuPower(0.0)
        assertEquals(0.0, computedPowerConsumption)
    }

    @ParameterizedTest
    @MethodSource("cpuPowerModelArgs")
    fun `emit power draw for hosts by different models`(
        powerModel: CpuPowerModel,
        expectedPowerConsumption: Double
    ) {
        runBlockingTest {
            val cpuLoads = flowOf(cpuUtil, cpuUtil, cpuUtil).stateIn(this)
            val machine = mockkClass(SimBareMetalMachine::class)
            every { machine.usage } returns cpuLoads

            val serverPowerDraw = powerModel.getPowerDraw(machine)

            assertEquals(
                serverPowerDraw.first().toDouble(),
                flowOf(expectedPowerConsumption).first().toDouble(),
                epsilon
            )

            verify(exactly = 1) { machine.usage }
        }
    }

    @Suppress("unused")
    private companion object {
        @JvmStatic
        fun cpuPowerModelArgs(): Stream<Arguments> = Stream.of(
            Arguments.of(ConstantPowerModel(0.0), 0.0),
            Arguments.of(LinearPowerModel(350.0, 200 / 350.0), 335.0),
            Arguments.of(SquarePowerModel(350.0, 200 / 350.0), 321.5),
            Arguments.of(CubicPowerModel(350.0, 200 / 350.0), 309.35),
            Arguments.of(SqrtPowerModel(350.0, 200 / 350.0), 342.302),
        )
    }
}
