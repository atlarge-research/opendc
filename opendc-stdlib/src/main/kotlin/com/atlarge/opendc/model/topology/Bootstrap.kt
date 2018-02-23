package com.atlarge.opendc.model.topology

import com.atlarge.opendc.simulator.Bootstrap
import com.atlarge.opendc.simulator.Entity

/**
 * Create a [Bootstrap] procedure for the given [Topology].
 *
 * @return A apply procedure for the topology.
 */
fun <T : Topology> T.bootstrap(): Bootstrap<T> = Bootstrap.create { ctx ->
    forEach { ctx.register(it) }
    listeners += object : TopologyListener {
        override fun Topology.onNodeAdded(node: Entity<*, Topology>) {
            ctx.register(node)
        }

        override fun Topology.onNodeRemoved(node: Entity<*, Topology>) {
            ctx.deregister(node)
        }
    }
    this
}
