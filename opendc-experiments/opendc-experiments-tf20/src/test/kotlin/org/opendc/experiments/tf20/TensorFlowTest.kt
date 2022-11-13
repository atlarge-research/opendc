/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.experiments.tf20

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.experiments.tf20.core.SimTFDevice
import org.opendc.experiments.tf20.distribute.MirroredStrategy
import org.opendc.experiments.tf20.distribute.OneDeviceStrategy
import org.opendc.experiments.tf20.util.MLEnvironmentReader
import org.opendc.simulator.compute.power.CpuPowerModels
import org.opendc.simulator.kotlin.runSimulation
import java.util.UUID

/**
 * Integration test suite for the TensorFlow application model in OpenDC.
 */
class TensorFlowTest {
    /**
     * Smoke test that tests the capabilities of the TensorFlow application model in OpenDC.
     */
    @Test
    fun testSmokeAlexNet() = runSimulation {
        val envInput = checkNotNull(TensorFlowTest::class.java.getResourceAsStream("/kth.json"))
        val def = MLEnvironmentReader().readEnvironment(envInput).first()

        val device = SimTFDevice(
            def.uid,
            def.meta["gpu"] as Boolean,
            dispatcher,
            def.model.cpus[0],
            def.model.memory[0],
            CpuPowerModels.linear(250.0, 60.0)
        )
        val strategy = OneDeviceStrategy(device)
        val batchSize = 32
        val model = AlexNet(batchSize.toLong())
        model.use {
            it.compile(strategy)

            it.fit(epochs = 9088 / batchSize, batchSize = batchSize)
        }

        device.close()

        val stats = device.getDeviceStats()
        assertAll(
            { assertEquals(3309694252, timeSource.millis()) },
            { assertEquals(8.27423563E8, stats.energyUsage) }
        )
    }

    /**
     * Smoke test that tests the capabilities of the TensorFlow application model in OpenDC.
     */
    @Test
    fun testSmokeVGG() = runSimulation {
        val envInput = checkNotNull(TensorFlowTest::class.java.getResourceAsStream("/kth.json"))
        val def = MLEnvironmentReader().readEnvironment(envInput).first()

        val device = SimTFDevice(
            def.uid,
            def.meta["gpu"] as Boolean,
            dispatcher,
            def.model.cpus[0],
            def.model.memory[0],
            CpuPowerModels.linear(250.0, 60.0)
        )
        val strategy = OneDeviceStrategy(device)
        val batchSize = 128
        val model = VGG16(batchSize.toLong())
        model.use {
            it.compile(strategy)

            it.fit(epochs = 9088 / batchSize, batchSize = batchSize)
        }

        device.close()

        val stats = device.getDeviceStats()
        assertAll(
            { assertEquals(176230328513, timeSource.millis()) },
            { assertEquals(4.405758212825E10, stats.energyUsage) }
        )
    }

    /**
     * Smoke test that tests the capabilities of the TensorFlow application model in OpenDC.
     */
    @Test
    fun testSmokeDistribute() = runSimulation {
        val envInput = checkNotNull(TensorFlowTest::class.java.getResourceAsStream("/kth.json"))
        val def = MLEnvironmentReader().readEnvironment(envInput).first()

        val deviceA = SimTFDevice(
            def.uid,
            def.meta["gpu"] as Boolean,
            dispatcher,
            def.model.cpus[0],
            def.model.memory[0],
            CpuPowerModels.linear(250.0, 60.0)
        )

        val deviceB = SimTFDevice(
            UUID.randomUUID(),
            def.meta["gpu"] as Boolean,
            dispatcher,
            def.model.cpus[0],
            def.model.memory[0],
            CpuPowerModels.linear(250.0, 60.0)
        )

        val strategy = MirroredStrategy(listOf(deviceA, deviceB))
        val batchSize = 32
        val model = AlexNet(batchSize.toLong())
        model.use {
            it.compile(strategy)

            it.fit(epochs = 9088 / batchSize, batchSize = batchSize)
        }

        deviceA.close()
        deviceB.close()

        val statsA = deviceA.getDeviceStats()
        val statsB = deviceB.getDeviceStats()
        assertAll(
            { assertEquals(1704994000, timeSource.millis()) },
            { assertEquals(4.262485E8, statsA.energyUsage) },
            { assertEquals(4.262485E8, statsB.energyUsage) }
        )
    }
}
