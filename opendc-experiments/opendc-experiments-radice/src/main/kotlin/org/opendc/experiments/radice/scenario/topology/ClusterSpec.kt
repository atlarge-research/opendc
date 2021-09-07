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

package org.opendc.experiments.radice.scenario.topology

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.opendc.experiments.radice.scenario.mapper.MachineSpecToStringConverter
import org.opendc.experiments.radice.scenario.mapper.StringToMachineSpecConverter

/**
 * Definition of a computing cluster modeled in the simulation.
 *
 * @param id A unique identifier representing the compute cluster.
 * @param name The name of the cluster.
 * @param machineModel The model of the machines in the cluster.
 * @param hostCount The number of hosts in the cluster.
 */
data class ClusterSpec(
    val id: String,
    val name: String,
    @JsonProperty("machine-model")
    @JsonSerialize(converter = MachineSpecToStringConverter::class)
    @JsonDeserialize(converter = StringToMachineSpecConverter::class)
    val machineModel: MachineSpec,
    @JsonProperty("host-count") val hostCount: Int
)
