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

package com.atlarge.opendc.model.odc.integration.jpa.schema

import com.atlarge.opendc.model.odc.topology.machine.Gpu
import javax.persistence.Entity

/**
 * A gpu entity in the persistent schema.
 *
 * @property id The unique identifier of the gpu.
 * @property manufacturer The manufacturer of the gpu.
 * @property family The family of the gpu.
 * @property generation The generation of the gpu.
 * @property model The model of the gpu.
 * @property clockRate The clock rate of the gpu.
 * @property cores The amount of cores in the gpu.
 * @property energyConsumption The energy consumption of the gpu in Watt.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
@Entity
data class Gpu(
	val id: Int,
	val manufacturer: String,
	val family: String,
	val generation: String,
	val model: String,
	override val clockRate: Int,
	override val cores: Int,
	override val energyConsumption: Double
) : Gpu {
	/**
	 * The initial state of the entity.
	 */
	override val initialState = Unit
}
