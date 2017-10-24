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

package nl.atlarge.opendc.integration.jpa.schema

import nl.atlarge.opendc.topology.container.Rack
import javax.persistence.Entity

/**
 * A rack entity in a room in the persistent schema.
 *
 * @property id The unique identifier of the rack.
 * @property name The name of the rack.
 * @property capacity The capacity of the rack in terms of units.
 * @property powerCapacity The power capacity of the rack in Watt.
 * @property machines The machines in the rack.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
@Entity
class Rack(
	id: Int,
	val name: String,
	val capacity: Int,
	val powerCapacity: Int,
	val machines: List<Machine>
): RoomObject(id), Rack {
	/**
	 * The initial state of the entity.
	 */
	override val initialState = Unit
}
