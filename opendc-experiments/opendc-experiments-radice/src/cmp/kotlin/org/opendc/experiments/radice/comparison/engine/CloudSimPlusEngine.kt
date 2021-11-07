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

package org.opendc.experiments.radice.comparison.engine

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple
import org.cloudbus.cloudsim.cloudlets.Cloudlet
import org.cloudbus.cloudsim.cloudlets.CloudletSimple
import org.cloudbus.cloudsim.core.CloudSim
import org.cloudbus.cloudsim.datacenters.Datacenter
import org.cloudbus.cloudsim.datacenters.DatacenterSimple
import org.cloudbus.cloudsim.hosts.Host
import org.cloudbus.cloudsim.hosts.HostSimple
import org.cloudbus.cloudsim.power.PowerMeasurement
import org.cloudbus.cloudsim.power.models.PowerModelHost
import org.cloudbus.cloudsim.resources.Pe
import org.cloudbus.cloudsim.resources.PeSimple
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelAbstract
import org.cloudbus.cloudsim.vms.Vm
import org.cloudbus.cloudsim.vms.VmSimple
import org.opendc.compute.workload.VirtualMachine
import org.opendc.experiments.radice.comparison.compat.VmSchedulerTimeSharedOverSubscription
import org.opendc.experiments.radice.scenario.topology.MachineSpec
import org.opendc.experiments.radice.scenario.topology.TopologySpec
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.power.PowerModel
import org.opendc.simulator.compute.workload.SimTrace
import org.opendc.simulator.flow.FlowConnection
import org.opendc.simulator.flow.FlowSource
import java.time.Duration
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Experiment for benchmarking CloudSim.
 */
class CloudSimPlusEngine : ExperimentEngine {
    private val HOST_BW: Long = 10000L // in Megabits/s
    private val HOST_STORAGE: Long = 1000000 // in Megabytes

    override val id: String = "cloudsim-plus"

    /**
     * Run the simulation.
     */
    override fun runScenario(workload: List<VirtualMachine>, topology: TopologySpec, seed: Long): ExperimentResult {
        val agg = ExperimentResultAggregator()
        val simulation = CloudSim()
        createDatacenter(simulation, topology, agg)

        // Creates a broker that is a software acting on behalf a cloud customer to manage his/her VMs and Cloudlets
        val broker = DatacenterBrokerSimple(simulation)

        val vms = mutableListOf<Vm>()
        val cloudlets = mutableListOf<Cloudlet>()

        require(workload.isNotEmpty()) { "Empty workload trace is given" }

        val startTime = workload.minOf { it.startTime }
        val stopTime = workload.maxOf { it.stopTime }
        val offset = startTime.toEpochMilli()

        for ((id, spec) in workload.withIndex()) {
            val (vm, cloudlet) = createVm(id, spec, offset)
            vms += vm
            cloudlets += cloudlet
        }

        broker.submitVmList(vms)
        broker.submitCloudletList(cloudlets)

        val duration = Duration.between(startTime, stopTime + Duration.ofMinutes(15))
        simulation.terminateAt(duration.toSeconds().toDouble())

        simulation.start()

        return agg
    }

    /**
     * Construct a [Datacenter] from the specified [simulation] and [topology].
     */
    private fun createDatacenter(simulation: CloudSim, topology: TopologySpec, agg: ExperimentResultAggregator): Datacenter {
        val hosts = mutableListOf<Host>()
        var id = 0L

        for (cluster in topology.clusters) {
            repeat(cluster.hostCount) {
                hosts += createHost(id++, cluster.machineModel, agg)
            }
        }

        // Uses a VmAllocationPolicySimple by default to allocate VMs
        val dc = DatacenterSimple(simulation, hosts)
        dc.vmAllocationPolicy = VmAllocationPolicySimple()
        dc.schedulingInterval = Duration.ofMinutes(5).toSeconds().toDouble() // Process every 5 minutes (granularity of the workload trace)
        return dc
    }

    /**
     * Construct a [Host] object from the specified [spec].
     */
    private fun createHost(id: Long, spec: MachineSpec, agg: ExperimentResultAggregator): Host {
        val peList = mutableListOf<Pe>()

        repeat(spec.cpuCount) {
            peList.add(PeSimple(spec.cpuCapacity * 1000.0))
        }

        val memCapacity = (spec.memCapacity * 1000.0).roundToLong()
        val powerModelHost = PowerModelHostAdapter(spec.powerModel)

        val host = HostSimple(memCapacity, HOST_BW, HOST_STORAGE, peList)
        host.vmScheduler = VmSchedulerTimeSharedOverSubscription()
        host.powerModel = powerModelHost
        // host.enableUtilizationStats()

        val idString = id.toString()
        var previousTime = Double.POSITIVE_INFINITY

        host.addOnStartupListener { event ->
            val time = event.time
            previousTime = time
        }
        host.addOnUpdateProcessingListener { event ->
            val time = event.time
            val delta = max(time - previousTime, 0.0)
            previousTime = time

            if (delta > 0.0) {
                val cpu = host.cpuPercentUtilization
                val power = powerModelHost.getPower(cpu)
                agg.record(idString, power * delta)
            }
        }

        host.addOnShutdownListener { event ->
            val time = event.time
            val delta = max(time - previousTime, 0.0)
            previousTime = time

            if (delta > 0.0) {
                val cpu = host.cpuPercentUtilization
                val power = powerModelHost.getPower(cpu)
                agg.record(idString, power * delta)
            }
        }

        return host
    }

    /**
     * Construct a [Vm] instance from the specified [spec].
     */
    private fun createVm(id: Int, spec: VirtualMachine, offset: Long): Pair<Vm, Cloudlet> {
        val vm = VmSimple(spec.cpuCapacity / spec.cpuCount, spec.cpuCount.toLong())
        vm
            .setRam(spec.memCapacity)
            .setBw(1)
            .setSize(1)
        vm.id = id.toLong()
        // vm.submissionDelay = (spec.startTime.toEpochMilli() - offset) / 1000.0 // Submission delay in seconds

        val length = spec.trace.length()
        val cloudlet = CloudletSimple(length, spec.cpuCount)

        val cpuNode = ProcessingNode("unknown", "unknown", "unknown", spec.cpuCount)
        val cpu = ProcessingUnit(cpuNode, 0, spec.cpuCapacity)
        val source = spec.trace.newSource(cpu, -offset)

        cloudlet.id = id.toLong()
        cloudlet.utilizationModelBw = UtilizationModel.NULL
        cloudlet.utilizationModelRam = UtilizationModel.NULL
        cloudlet.utilizationModelCpu = UtilizationModelTrace(cpu, source, cloudlet)

        cloudlet.vm = vm

        return vm to cloudlet
    }

    /**
     * Compute the length of a trace.
     */
    private fun SimTrace.length(): Long {
        var length = 0L

        for (i in 0 until size) {
            val usage = usageCol[i].coerceAtLeast(0.03)
            val cores = coresCol[i]
            val duration = deadlineCol[i] - timestampCol[i]

            length += (usage * cores * (duration / 1000.0)).roundToLong()
        }

        return length
    }

    /**
     * An adapter class for a [SimTrace] to a [UtilizationModelAbstract].
     */
    class UtilizationModelTrace(
        private val cpu: ProcessingUnit,
        private val source: FlowSource,
        private val cloudlet: Cloudlet
    ) : UtilizationModelAbstract(UtilizationModel.Unit.ABSOLUTE), FlowConnection {
        /**
         * The current utilization set by [source].
         */
        private var _utilization = 0.0

        /**
         * The last timestamp at which the utilization was requested.
         */
        private var _lastTimestamp = Long.MAX_VALUE

        /**
         * A flag to indicate that the source was started.
         */
        private var _isStarted = false

        init {
            isOverCapacityRequestAllowed = true
        }

        override fun getUtilizationInternal(time: Double): Double {
            val now = ((time - cloudlet.execStartTime) * 1000.0).roundToLong()
            val delta = max(0L, now - _lastTimestamp)
            _lastTimestamp = now

            // Offset the target by 5min, since CloudSim utilization model is called after 5min, while OpenDC's workload
            // is called at the start of the 5min
            val target = now - 300_000

            // Determine whether [source.onPull] has already been called once or not
            val shouldStart = if (!_isStarted) {
                _isStarted = true
                true
            } else {
                false
            }

            if (delta > 0 || shouldStart) {
                source.onPull(this, target)
            }

            val utilization = _utilization

            // BUG: If we return a usage of zero, CloudSim will stop processing the Cloudlet. Return a very small
            // number to continue processing the Cloudlet while indicating that it is not doing anything meaningful
            // at this timestamp.
            return utilization.coerceAtLeast(0.01)
        }

        override val capacity: Double
            get() = cpu.frequency
        override val rate: Double
            get() = _utilization
        override val demand: Double
            get() = _utilization
        override var shouldSourceConverge: Boolean
            get() = false
            set(_) { throw UnsupportedOperationException() }

        override fun pull() {
            throw UnsupportedOperationException()
        }

        override fun pull(now: Long) = pull()

        override fun push(rate: Double) {
            _utilization = rate
        }

        override fun close() {
            // Cloudlet is finished
            cloudlet.addFinishedLengthSoFar(Long.MAX_VALUE)

            _utilization = 0.0
        }
    }

    private class ExperimentResultAggregator : ExperimentResult {
        override val powerConsumption: MutableMap<String, Double> = mutableMapOf()

        fun record(hostId: String, power: Double) {
            powerConsumption.merge(hostId, power, Double::plus)
        }
    }

    /**
     * A conversion class from a [PowerModel] to [PowerModelHost].
     */
    private class PowerModelHostAdapter(private val powerModel: PowerModel) : PowerModelHost() {
        /**
         * The idle power of the host.
         */
        private val idle = powerModel.computePower(0.0)

        override fun getPowerMeasurement(): PowerMeasurement {
            return PowerMeasurement(idle, power - idle)
        }

        override fun getPower(utilizationFraction: Double): Double {
            return powerModel.computePower(utilizationFraction)
        }
    }
}
