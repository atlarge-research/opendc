/*
 * MIT License
 *
 * Copyright (c) 2017 atlarge-research
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

package com.atlarge.opendc.model.odc.topology

import com.atlarge.opendc.model.odc.integration.jpa.schema.Rack
import com.atlarge.opendc.model.odc.integration.jpa.schema.Room
import com.atlarge.opendc.model.odc.integration.jpa.schema.RoomObject
import com.atlarge.opendc.model.odc.integration.jpa.schema.Section
import com.atlarge.opendc.model.topology.AdjacencyList
import com.atlarge.opendc.model.topology.Topology
import com.atlarge.opendc.model.topology.MutableTopology
import com.atlarge.opendc.model.topology.TopologyBuilder
import com.atlarge.opendc.model.topology.TopologyFactory

/**
 * A [TopologyFactory] that converts a [Section] of an experiment as defined by the API, into a proper [Topology].
 *
 * @property section The section to convert into a topology.
 * @property builder A builder for a topology to use.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
class JpaTopologyFactory(val section: Section, val builder: TopologyBuilder = AdjacencyList.builder()) : TopologyFactory {
	/**
	 * Create a [MutableTopology] instance.
	 *
	 * @return A mutable topology.
	 */
	override fun create(): MutableTopology = builder.construct {
		val datacenter = section.datacenter
		add(datacenter)
		datacenter.rooms.forEach { room ->
			add(room)
			connect(datacenter, room, tag = "room")

			room.objects.forEach { roomObject(room, it) }
		}
	}

	/**
	 * Handle the objects in a room.
	 *
	 * @param obj The obj to handle.
	 */
	private fun MutableTopology.roomObject(parent: Room, obj: RoomObject) = when(obj) {
		is Rack -> rack(parent, obj)
		else -> Unit
	}

	/**
	 * Handle a rack in a room.
	 *
	 * @param parent The parent of the rack.
	 * @param rack The rack to handle.
	 */
	private fun MutableTopology.rack(parent: Room, rack: Rack) {
		add(rack)
		connect(parent, rack, tag = "rack")
		rack.machines.forEach { machine ->
			add(machine)
			connect(rack, machine, tag = "machine")

			machine.cpus.forEach { cpu ->
				add(cpu)
				connect(machine, cpu, tag = "cpu")
			}

			machine.gpus.forEach { gpu ->
				add(gpu)
				connect(machine, gpu, tag = "gpu")
			}
		}
	}
}
