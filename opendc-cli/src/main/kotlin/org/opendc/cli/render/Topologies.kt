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
import org.opendc.sdk.model.experiment.Experiment
import org.opendc.sdk.model.experiment.expand
import org.opendc.sdk.model.topology.Host
import org.opendc.sdk.model.topology.Topology

/** Prints a one-line experiment header followed by every topology it declares. */
internal fun renderTopologies(
    terminal: Terminal,
    experiment: Experiment,
) {
    val name = experiment.name.ifEmpty { "(unnamed)" }
    terminal.println(
        "Experiment ${terminal.theme.info(name)}: ${experiment.topologies.size} topolog(ies), " +
            "${experiment.workloads.size} workload(s), ${experiment.allocationPolicies.size} policy(ies), " +
            "${experiment.expand().size} scenario(s)",
    )
    experiment.topologies.forEachIndexed { index, topology -> renderTopology(terminal, index, topology) }
}

private fun renderTopology(
    terminal: Terminal,
    index: Int,
    topology: Topology,
) {
    terminal.println("")
    terminal.println("${terminal.theme.info("Topology #$index")} — ${topology.hostCount} host(s)")
    terminal.println(
        table {
            header { row("Cluster", "Host", "Count", "CPU", "Memory", "GPU") }
            body {
                for (cluster in topology.clusters) {
                    val clusterLabel = if (cluster.count > 1) "${cluster.name} ×${cluster.count}" else cluster.name
                    for (host in cluster.hosts) {
                        row(clusterLabel, host.name, host.count, host.describeCpu(), host.memory.size, host.describeGpu())
                    }
                }
            }
        },
    )
}

private val Topology.hostCount: Int get() = clusters.sumOf { c -> c.count * c.hosts.sumOf { it.count } }

private fun Host.describeCpu(): String = "${cpu.count}× ${cpu.coreCount} cores @ ${cpu.coreSpeed}"

private fun Host.describeGpu(): String = gpu?.let { "${it.count}× ${it.coreCount} cores @ ${it.coreSpeed}" } ?: "—"
