/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

package com.atlarge.opendc.experiments.sc20

import com.atlarge.odcsim.SimulationEngineProvider
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.core.monitor.ServerMonitor
import com.atlarge.opendc.compute.metal.service.ProvisioningService
import com.atlarge.opendc.compute.virt.service.SimpleVirtProvisioningService
import com.atlarge.opendc.format.environment.sc18.Sc18EnvironmentReader
import com.atlarge.opendc.format.trace.vm.VmTraceReader
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.ServiceLoader
import kotlin.math.max

/**
 * Main entry point of the experiment.
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("error: Please provide path to directory containing VM trace files")
        return
    }

    val environment = Sc18EnvironmentReader(object {}.javaClass.getResourceAsStream("/env/setup-test.json"))
        .use { it.read() }

    val token = Channel<Boolean>()

    val monitor = object : ServerMonitor {
        override suspend fun onUpdate(server: Server, previousState: ServerState) {
            println(server)
        }
    }

    val provider = ServiceLoader.load(SimulationEngineProvider::class.java).first()
    val system = provider({ ctx ->
        println(ctx.clock.instant())
        val scheduler = SimpleVirtProvisioningService(
            ctx,
            environment.platforms[0].zones[0].services[ProvisioningService.Key],
            Sc20HypervisorMonitor()
        )

        val reader = VmTraceReader(File(args[0]))
        delay(1376314846 * 1000L)
        while (reader.hasNext()) {
            val (time, workload) = reader.next()
            delay(max(0, time * 1000 - ctx.clock.millis()))
            scheduler.deploy(workload.image, monitor)
        }

        token.receive()

        println(ctx.clock.instant())
    }, name = "sim")

    runBlocking {
        system.run()
        system.terminate()
    }
}
