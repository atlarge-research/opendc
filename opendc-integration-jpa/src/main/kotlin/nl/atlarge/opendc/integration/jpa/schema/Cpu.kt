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

import nl.atlarge.opendc.topology.machine.Cpu
import javax.persistence.Entity

/**
 * A cpu entity in the persistent schema.
 *
 * @property id The unique identifier of the cpu.
 * @property manufacturer The manufacturer of the cpu.
 * @property family The family of the cpu.
 * @property generation The generation of the cpu.
 * @property model The model of the cpu.
 * @property clockRate The clock rate of the cpu.
 * @property cores The amount of cores in the gpu.
 * @property energyConsumption The energy consumption of the cpu in Watt.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
@Entity
data class Cpu(
	val id: Int,
	val manufacturer: String,
	val family: String,
	val generation: String,
	val model: String,
	override val clockRate: Int,
	override val cores: Int,
	override val energyConsumption: Double
) : Cpu {
	/**
	 * The initial state of the entity.
	 */
	override val initialState = Unit
}
