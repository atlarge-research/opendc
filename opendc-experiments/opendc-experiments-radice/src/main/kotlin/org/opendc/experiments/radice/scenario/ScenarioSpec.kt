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

package org.opendc.experiments.radice.scenario

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.typesafe.config.Config
import org.opendc.experiments.radice.scenario.mapper.StringToWorkloadSpecConverter
import org.opendc.experiments.radice.scenario.mapper.WorkloadSpecToStringConverter
import org.opendc.experiments.radice.scenario.topology.BASE_TOPOLOGY
import org.opendc.experiments.radice.scenario.topology.TopologySpec

/**
 * A risk analysis scenario.
 *
 * @property workload The workload to experiment with.
 * @property topology The topology of the datacenter.
 * @property scheduler The scheduler of the datacenter.
 * @property phenomena The operational phenomena to occur during th experiment.
 * @property availabilitySla The availability SLA used in the experiment.
 * @property qosSla The QoS SLA used in the experiment.
 * @property pue The PUE of the datacenter.
 * @property carbonFootprint The amount of CO2 emissions per kWh of electricity.
 * @property costs The costs of operating the datacenter.
 * @property recordInterference A flag to indicate that the interference should be recorded.
 * @property partitions The partition of the portfolio.
 */
data class ScenarioSpec(
    @JsonSerialize(converter = WorkloadSpecToStringConverter::class)
    @JsonDeserialize(converter = StringToWorkloadSpecConverter::class)
    val workload: WorkloadSpec,
    val topology: TopologySpec,
    val scheduler: SchedulerSpec,
    val phenomena: OperationalPhenomena,
    @JsonProperty("availability-sla") val availabilitySla: ServiceLevelAgreement,
    @JsonProperty("qos-sla") val qosSla: ServiceLevelAgreement,
    val pue: Double,
    @JsonProperty("co2-footprint") val carbonFootprint: Double,
    val costs: DatacenterCosts,
    @JsonProperty("record-interference") val recordInterference: Boolean = false,
    val partitions: Map<String, String> = emptyMap()
) {
    companion object {
        /**
         * Construct the default [ScenarioSpec] based on the specified [config].
         */
        @JvmStatic
        fun fromDefaults(
            config: Config,
            workload: WorkloadSpec = WorkloadSpec("baseline"),
            topology: TopologySpec = BASE_TOPOLOGY,
            scheduler: SchedulerSpec = SCHEDULER_DEFAULT,
            hasInterference: Boolean = (workload.name == "baseline"),
            partitions: Map<String, String> = emptyMap()
        ): ScenarioSpec {
            val dc = config.getConfig("datacenter")
            val costConfig = config.getConfig("costs")

            val availabilityRefunds = costConfig.getConfig("availability").entrySet().associate { it.key.toDouble() to it.value.render().toDouble() }
            val qosRefunds = costConfig.getConfig("qos").entrySet().associate { it.key.toDouble() to it.value.render().toDouble() }

            return ScenarioSpec(
                workload,
                topology,
                scheduler,
                OperationalPhenomena(config.getDuration("phenomena.failure-interval"), hasInterference),
                availabilitySla = ServiceLevelAgreement(
                    target = dc.getDouble("availability-target"),
                    refunds = availabilityRefunds,
                ),
                qosSla = ServiceLevelAgreement(
                    target = dc.getDouble("qos-target"),
                    refunds = qosRefunds
                ),
                pue = dc.getDouble("pue"),
                carbonFootprint = dc.getDouble("co2-footprint"),
                costs = DatacenterCosts(
                    costPerCpuH = costConfig.getDouble("vcpu"),
                    powerCost = costConfig.getDouble("power"),
                    carbonCost = costConfig.getDouble("co2"),
                    carbonSocialCost = costConfig.getDouble("co2-social"),
                    investigationCost = costConfig.getDouble("investigation")
                ),
                partitions = partitions
            )
        }
    }
}
