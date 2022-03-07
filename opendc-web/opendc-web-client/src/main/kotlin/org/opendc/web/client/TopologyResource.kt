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

package org.opendc.web.client

import org.opendc.web.client.internal.delete
import org.opendc.web.client.internal.get
import org.opendc.web.client.internal.post
import org.opendc.web.client.internal.put
import org.opendc.web.client.transport.TransportClient
import org.opendc.web.proto.user.Topology

/**
 * A resource representing the topologies available to the user.
 */
public class TopologyResource internal constructor(private val client: TransportClient) {
    /**
     * List all topologies that belong to the specified [project].
     */
    public fun getAll(project: Long): List<Topology> = client.get("projects/$project/topologies") ?: emptyList()

    /**
     * Obtain the topology for [project] with [index].
     */
    public fun get(project: Long, index: Int): Topology? = client.get("projects/$project/topologies/$index")

    /**
     * Create a new topology for [project] with [request].
     */
    public fun create(project: Long, request: Topology.Create): Topology {
        return checkNotNull(client.post("projects/$project/topologies", request))
    }

    /**
     * Update the topology with [index] for [project] using the specified [request].
     */
    public fun update(project: Long, index: Int, request: Topology.Update): Topology? {
        return client.put("projects/$project/topologies/$index", request)
    }

    /**
     * Delete the topology for [project] with [index].
     */
    public fun delete(project: Long, index: Long): Topology {
        return requireNotNull(client.delete("projects/$project/topologies/$index")) { "Unknown topology $index" }
    }
}
