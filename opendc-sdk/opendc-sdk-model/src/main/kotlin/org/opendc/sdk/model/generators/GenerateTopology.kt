package org.opendc.sdk.model.generators

import org.opendc.common.units.DataSize
import org.opendc.common.units.Frequency
import org.opendc.sdk.model.topology.ClusterSpec
import org.opendc.sdk.model.topology.CpuSpec
import org.opendc.sdk.model.topology.HostSpec
import org.opendc.sdk.model.topology.MemorySpec
import org.opendc.sdk.model.topology.TopologySpec

public fun generateTopology(numHosts: Int): TopologySpec {
    return TopologySpec(
        clusters = listOf(
            ClusterSpec(
                hosts = listOf(
                    HostSpec(
                        count = numHosts,
                        cpu = CpuSpec(
                            coreCount = 16,
                            coreSpeed = Frequency.ofMHz(2100),
                        ),
                        memory = MemorySpec(
                            size = DataSize.ofMiB(10000.0),
                        )
                    )
                )
            )
        )
    )
}
