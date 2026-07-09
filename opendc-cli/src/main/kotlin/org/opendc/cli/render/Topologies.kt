/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.cli.render

import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import org.opendc.cli.config.CliConfig
import org.opendc.sdk.model.experiment.Experiment
import org.opendc.sdk.model.experiment.expand
import org.opendc.sdk.model.topology.Host
import org.opendc.sdk.model.topology.Topology

/** One table row describing a host group within a topology. */
internal data class TopologyRow(
    val cluster: String,
    val host: String,
    val count: Int,
    val cpu: String,
    val memory: String,
    val gpu: String,
)

/** One topology: its index, total host count, and per-host-group rows. */
internal data class TopologyEntry(
    val index: Int,
    val hostCount: Int,
    val rows: List<TopologyRow>,
)

/**
 * The render-ready view of every topology an experiment declares. Symbols (× separators, the em-dash
 * for a missing GPU) are baked in from the [CliConfig] at construction, so rendering only lays it out.
 */
internal data class TopologyView(
    val name: String,
    val topologies: Int,
    val workloads: Int,
    val policies: Int,
    val scenarios: Int,
    val entries: List<TopologyEntry>,
) {
    companion object {
        fun from(
            experiment: Experiment,
            config: CliConfig,
        ): TopologyView =
            TopologyView(
                name = experiment.name.ifEmpty { "(unnamed)" },
                topologies = experiment.topologies.size,
                workloads = experiment.workloads.size,
                policies = experiment.allocationPolicies.size,
                scenarios = experiment.expand().size,
                entries = experiment.topologies.mapIndexed { index, topology -> topology.toEntry(index, config) },
            )
    }
}

/** Prints a one-line experiment header followed by every topology it declares. */
internal fun renderTopologies(
    terminal: Terminal,
    view: TopologyView,
    config: CliConfig,
) {
    terminal.println(
        "Experiment ${terminal.theme.info(view.name)}: ${view.topologies} topolog(ies), " +
            "${view.workloads} workload(s), ${view.policies} policy(ies), " +
            "${view.scenarios} scenario(s)",
    )
    view.entries.forEach { entry -> renderTopology(terminal, entry, config) }
}

private fun renderTopology(
    terminal: Terminal,
    entry: TopologyEntry,
    config: CliConfig,
) {
    terminal.println("")
    terminal.println("${terminal.theme.info("Topology #${entry.index}")} ${config.symbols.dash} ${entry.hostCount} host(s)")
    terminal.println(
        table {
            header { row(*config.headers.topology.toTypedArray()) }
            body {
                for (r in entry.rows) {
                    row(r.cluster, r.host, r.count, r.cpu, r.memory, r.gpu)
                }
            }
        },
    )
}

private fun Topology.toEntry(
    index: Int,
    config: CliConfig,
): TopologyEntry =
    TopologyEntry(
        index = index,
        hostCount = clusters.sumOf { c -> c.count * c.hosts.sumOf { it.count } },
        rows =
            clusters.flatMap { cluster ->
                val clusterLabel =
                    if (cluster.count > 1) "${cluster.name} ${config.symbols.times}${cluster.count}" else cluster.name
                cluster.hosts.map { host ->
                    TopologyRow(
                        cluster = clusterLabel,
                        host = host.name,
                        count = host.count,
                        cpu = host.describeCpu(config),
                        memory = host.memory.size.toString(),
                        gpu = host.describeGpu(config),
                    )
                }
            },
    )

private fun Host.describeCpu(config: CliConfig): String = "${cpu.count}${config.symbols.times} ${cpu.coreCount} cores @ ${cpu.coreSpeed}"

private fun Host.describeGpu(config: CliConfig): String =
    gpu?.let { "${it.count}${config.symbols.times} ${it.coreCount} cores @ ${it.coreSpeed}" } ?: config.symbols.dash
