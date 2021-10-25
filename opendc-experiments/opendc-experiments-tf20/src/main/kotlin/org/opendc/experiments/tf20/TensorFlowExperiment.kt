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

package org.opendc.experiments.tf20

import io.opentelemetry.api.metrics.MeterProvider
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import org.opendc.experiments.tf20.core.SimTFDevice
import org.opendc.experiments.tf20.distribute.*
import org.opendc.experiments.tf20.keras.AlexNet
import org.opendc.experiments.tf20.util.MLEnvironmentReader
import org.opendc.harness.dsl.Experiment
import org.opendc.harness.dsl.anyOf
import org.opendc.simulator.compute.power.LinearPowerModel
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.telemetry.sdk.toOtelClock

/**
 * Experiments with the TensorFlow simulation model.
 */
public class TensorFlowExperiment : Experiment(name = "tf20") {
    /**
     * The environment file to use.
     */
    private val environmentFile by anyOf("/kth.json")

    /**
     * The batch size used.
     */
    private val batchSize by anyOf(16, 32, 64, 128)

    override fun doRun(repeat: Int): Unit = runBlockingSimulation {
        val meterProvider: MeterProvider = SdkMeterProvider
            .builder()
            .setClock(clock.toOtelClock())
            .build()
        val meter = meterProvider.get("opendc-tf20")

        val envInput = checkNotNull(TensorFlowExperiment::class.java.getResourceAsStream(environmentFile))
        val def = MLEnvironmentReader().readEnvironment(envInput).first()
        val device = SimTFDevice(
            def.uid, def.meta["gpu"] as Boolean, coroutineContext, clock, meter, def.model.cpus[0],
            def.model.memory[0], LinearPowerModel(250.0, 60.0)
        )
        val strategy = OneDeviceStrategy(device)

        val model = AlexNet(batchSize.toLong())
        model.use {
            it.compile(strategy)

            it.fit(epochs = 9088 / batchSize, batchSize = batchSize)
        }
    }
}
