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

package org.opendc.compute.simulator.provisioner

import org.opendc.compute.carbon.getCarbonFragments
import org.opendc.compute.simulator.host.SimHost
import org.opendc.compute.simulator.service.ComputeService
import org.opendc.compute.topology.specs.ClusterSpec
import org.opendc.compute.topology.specs.HostSpec
import org.opendc.simulator.compute.power.CarbonModel
import org.opendc.simulator.compute.power.SimPowerSource
import org.opendc.simulator.compute.power.batteries.BatteryAggregator
import org.opendc.simulator.compute.power.batteries.SimBattery
import org.opendc.simulator.compute.power.batteries.policy.SingleThresholdBatteryPolicy
import org.opendc.simulator.engine.engine.FlowEngine
import org.opendc.simulator.engine.graph.FlowDistributor

/**
 * A [ProvisioningStep] that provisions a list of hosts for a [ComputeService].
 *
 * @param serviceDomain The domain name under which the compute service is registered.
 * @param clusterSpecs A list of [HostSpec] objects describing the simulated hosts to provision.
 * @param startTime The absolute start time of the simulation. Used to determine the carbon trace offset.
 */
public class HostsProvisioningStep internal constructor(
    private val serviceDomain: String,
    private val clusterSpecs: List<ClusterSpec>,
    private val startTime: Long = 0L,
) : ProvisioningStep {
    override fun apply(ctx: ProvisioningContext): AutoCloseable {
        val service =
            requireNotNull(
                ctx.registry.resolve(serviceDomain, ComputeService::class.java),
            ) { "Compute service $serviceDomain does not exist" }
        val simHosts = mutableSetOf<SimHost>()
        val simPowerSources = mutableListOf<SimPowerSource>()

        val engine = FlowEngine.create(ctx.dispatcher)
        val graph = engine.newGraph()

        for (cluster in clusterSpecs) {
            // Create the Power Source to which hosts are connected

            // Create Power Source
            val simPowerSource = SimPowerSource(graph, cluster.powerSource.totalPower.toDouble(), cluster.powerSource.name, cluster.name)
            simPowerSources.add(simPowerSource)
            service.addPowerSource(simPowerSource)

            val hostDistributor = FlowDistributor(graph)

            val carbonFragments = getCarbonFragments(cluster.powerSource.carbonTracePath)

            var carbonModel: CarbonModel? = null
            // Create Carbon Model
            if (carbonFragments != null) {
                carbonModel = CarbonModel(graph, carbonFragments, startTime)
                carbonModel.addReceiver(simPowerSource)
            }

            if (cluster.battery != null) {
                // Create Battery Distributor
                val batteryDistributor = FlowDistributor(graph)
                graph.addEdge(batteryDistributor, simPowerSource)

                // Create Battery
                val battery =
                    SimBattery(
                        graph,
                        cluster.battery!!.capacity,
                        cluster.battery!!.chargingSpeed,
                        cluster.battery!!.initialCharge,
                        cluster.battery!!.name,
                        cluster.name,
                    )
                graph.addEdge(battery, batteryDistributor)

                // Create Aggregator
                val batteryAggregator = BatteryAggregator(graph, battery, batteryDistributor)

                // Create BatteryPolicy
                val batteryPolicy =
                    SingleThresholdBatteryPolicy(
                        graph,
                        battery,
                        batteryAggregator,
                        cluster.battery!!.batteryPolicy.carbonThreshold,
                    )

                carbonModel?.addReceiver(batteryPolicy)

                graph.addEdge(hostDistributor, batteryAggregator)

                service.addBattery(battery)
            } else {
                graph.addEdge(hostDistributor, simPowerSource)
            }

            // Create hosts, they are connected to the powerMux when SimMachine is created
            for (hostSpec in cluster.hostSpecs) {
                val simHost =
                    SimHost(
                        hostSpec.name,
                        cluster.name,
                        ctx.dispatcher.timeSource,
                        graph,
                        hostSpec.model,
                        hostSpec.cpuPowerModel,
                        hostDistributor,
                    )

                require(simHosts.add(simHost)) { "Host with name ${hostSpec.name} already exists" }
                service.addHost(simHost)
            }
        }

        return AutoCloseable {
            for (simHost in simHosts) {
                simHost.close()
            }

            for (simPowerSource in simPowerSources) {
                simPowerSource.close()
            }
        }
    }
}
