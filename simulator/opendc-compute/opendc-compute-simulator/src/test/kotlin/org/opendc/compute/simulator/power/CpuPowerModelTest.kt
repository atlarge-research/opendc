package org.opendc.compute.simulator.power

import io.mockk.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.opendc.compute.simulator.power.api.CpuPowerModel
import org.opendc.compute.simulator.power.models.*
import org.opendc.metal.driver.BareMetalDriver
import java.util.stream.Stream
import kotlin.math.pow

internal class CpuPowerModelTest {
    private val epsilon = 10.0.pow(-3)
    private val cpuUtil = .9

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
        val cpuLoads = flowOf(cpuUtil, cpuUtil, cpuUtil)
        val bareMetalDriver = mockkClass(BareMetalDriver::class)
        every { bareMetalDriver.usage } returns cpuLoads

        runBlocking {
            val serverPowerDraw = powerModel.getPowerDraw(bareMetalDriver)

            assertEquals(serverPowerDraw.count(), cpuLoads.count())
            assertEquals(
                serverPowerDraw.first().toDouble(),
                flowOf(expectedPowerConsumption).first().toDouble(),
                epsilon
            )
        }
        verify(exactly = 1) { bareMetalDriver.usage }
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
