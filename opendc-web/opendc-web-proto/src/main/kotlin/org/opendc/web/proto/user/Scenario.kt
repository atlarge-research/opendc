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

package org.opendc.web.proto.user

import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.opendc.web.proto.OperationalPhenomena
import org.opendc.web.proto.Workload
import javax.validation.constraints.NotBlank

/**
 * A single scenario to be explored by the simulator.
 */
public data class Scenario(
    val id: Long,
    val number: Int,
    val project: Project,
    val portfolio: Portfolio.Summary,
    val name: String,
    val workload: Workload,
    val topology: Topology.Summary,
    val phenomena: OperationalPhenomena,
    val schedulerName: String,
    val job: Job
) {
    /**
     * Create a new scenario.
     *
     * @param name The name of the scenario.
     * @param workload The workload specification to use for the scenario.
     * @param topology The number of the topology to use.
     * @param phenomena The phenomena to model during simulation.
     * @param schedulerName The name of the scheduler.
     */
    @Schema(name = "Scenario.Create")
    public data class Create(
        @field:NotBlank(message = "Name must not be empty")
        val name: String,
        val workload: Workload.Spec,
        val topology: Long,
        val phenomena: OperationalPhenomena,
        val schedulerName: String,
    )

    /**
     * A summary view of a [Scenario] provided for nested relations.
     *
     * @param id The unique identifier of the scenario.
     * @param number The number of the scenario for the project.
     * @param name The name of the scenario.
     * @param workload The workload to be modeled by the scenario.
     * @param phenomena The phenomena simulated for this scenario.
     * @param schedulerName The scheduler name to use for the experiment.
     * @param job The simulation job associated with the scenario.
     */
    @Schema(name = "Scenario.Summary")
    public data class Summary(
        val id: Long,
        val number: Int,
        val name: String,
        val workload: Workload,
        val topology: Topology.Summary,
        val phenomena: OperationalPhenomena,
        val schedulerName: String,
        val job: Job
    )
}
