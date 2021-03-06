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

package org.opendc.experiments.energy21

import com.typesafe.config.ConfigFactory
import io.opentelemetry.api.metrics.MeterProvider
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.MetricProducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging
import org.opendc.compute.service.ComputeService
import org.opendc.compute.service.scheduler.ComputeScheduler
import org.opendc.compute.service.scheduler.FilterScheduler
import org.opendc.compute.service.scheduler.filters.ComputeCapabilitiesFilter
import org.opendc.compute.service.scheduler.filters.ComputeFilter
import org.opendc.compute.service.scheduler.weights.RandomWeigher
import org.opendc.compute.simulator.SimHost
import org.opendc.experiments.capelin.*
import org.opendc.experiments.capelin.monitor.ParquetExperimentMonitor
import org.opendc.experiments.capelin.trace.Sc20StreamingParquetTraceReader
import org.opendc.harness.dsl.Experiment
import org.opendc.harness.dsl.anyOf
import org.opendc.simulator.compute.SimFairShareHypervisorProvider
import org.opendc.simulator.compute.SimMachineModel
import org.opendc.simulator.compute.cpufreq.*
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.power.*
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.telemetry.sdk.toOtelClock
import java.io.File
import java.time.Clock
import java.util.*
import kotlin.random.asKotlinRandom

/**
 * Experiments for the OpenDC project on Energy modeling.
 */
public class EnergyExperiment : Experiment("Energy Modeling 2021") {
    /**
     * The logger for this portfolio instance.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The configuration to use.
     */
    private val config = ConfigFactory.load().getConfig("opendc.experiments.energy21")

    /**
     * The traces to test.
     */
    private val trace by anyOf("solvinity")

    /**
     * The power models to test.
     */
    private val powerModel by anyOf(PowerModelType.LINEAR, PowerModelType.CUBIC, PowerModelType.INTERPOLATION)

    override fun doRun(repeat: Int): Unit = runBlockingSimulation {
        val chan = Channel<Unit>(Channel.CONFLATED)
        val allocationPolicy = FilterScheduler(
            filters = listOf(ComputeFilter(), ComputeCapabilitiesFilter()),
            weighers = listOf(RandomWeigher(Random(0)) to 1.0)
        )

        val meterProvider: MeterProvider = SdkMeterProvider
            .builder()
            .setClock(clock.toOtelClock())
            .build()

        val monitor = ParquetExperimentMonitor(File(config.getString("output-path")), "power_model=$powerModel/run_id=$repeat", 4096)
        val trace = Sc20StreamingParquetTraceReader(File(config.getString("trace-path"), trace), random = Random(1).asKotlinRandom())

        withComputeService(clock, meterProvider, allocationPolicy) { scheduler ->
            withMonitor(monitor, clock, meterProvider as MetricProducer, scheduler) {
                processTrace(
                    clock,
                    trace,
                    scheduler,
                    chan,
                    monitor
                )
            }
        }

        val monitorResults = collectMetrics(meterProvider as MetricProducer)
        logger.debug {
            "Finish SUBMIT=${monitorResults.submittedVms} " +
                "FAIL=${monitorResults.unscheduledVms} " +
                "QUEUE=${monitorResults.queuedVms} " +
                "RUNNING=${monitorResults.runningVms}"
        }
    }

    /**
     * Construct the environment for a simulated compute service..
     */
    public suspend fun withComputeService(
        clock: Clock,
        meterProvider: MeterProvider,
        scheduler: ComputeScheduler,
        block: suspend CoroutineScope.(ComputeService) -> Unit
    ): Unit = coroutineScope {
        val model = createMachineModel()
        val hosts = List(64) { id ->
            SimHost(
                UUID(0, id.toLong()),
                "node-$id",
                model,
                emptyMap(),
                coroutineContext,
                clock,
                meterProvider.get("opendc-compute-simulator"),
                SimFairShareHypervisorProvider(),
                PerformanceScalingGovernor(),
                powerModel.driver
            )
        }

        val serviceMeter = meterProvider.get("opendc-compute")
        val service =
            ComputeService(coroutineContext, clock, serviceMeter, scheduler)

        for (host in hosts) {
            service.addHost(host)
        }

        try {
            block(this, service)
        } finally {
            service.close()
            hosts.forEach(SimHost::close)
        }
    }

    /**
     * The machine model based on: https://www.spec.org/power_ssj2008/results/res2020q1/power_ssj2008-20191125-01012.html
     */
    private fun createMachineModel(): SimMachineModel {
        val node = ProcessingNode("AMD", "am64", "EPYC 7742", 64)
        val cpus = List(node.coreCount) { id -> ProcessingUnit(node, id, 3400.0) }
        val memory = List(8) { MemoryUnit("Samsung", "Unknown", 2933.0, 16_000) }

        return SimMachineModel(cpus, memory)
    }

    /**
     * The power models to test.
     */
    public enum class PowerModelType {
        CUBIC {
            override val driver: ScalingDriver = SimpleScalingDriver(CubicPowerModel(206.0, 56.4))
        },

        LINEAR {
            override val driver: ScalingDriver = SimpleScalingDriver(LinearPowerModel(206.0, 56.4))
        },

        SQRT {
            override val driver: ScalingDriver = SimpleScalingDriver(SqrtPowerModel(206.0, 56.4))
        },

        SQUARE {
            override val driver: ScalingDriver = SimpleScalingDriver(SquarePowerModel(206.0, 56.4))
        },

        INTERPOLATION {
            override val driver: ScalingDriver = SimpleScalingDriver(
                InterpolationPowerModel(
                    listOf(56.4, 100.0, 107.0, 117.0, 127.0, 138.0, 149.0, 162.0, 177.0, 191.0, 206.0)
                )
            )
        },

        MSE {
            override val driver: ScalingDriver = SimpleScalingDriver(MsePowerModel(206.0, 56.4, 1.4))
        },

        ASYMPTOTIC {
            override val driver: ScalingDriver = SimpleScalingDriver(AsymptoticPowerModel(206.0, 56.4, 0.3, false))
        },

        ASYMPTOTIC_DVFS {
            override val driver: ScalingDriver = SimpleScalingDriver(AsymptoticPowerModel(206.0, 56.4, 0.3, true))
        };

        public abstract val driver: ScalingDriver
    }
}
