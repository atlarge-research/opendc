/*
 * Copyright (c) 2023 AtLarge Research
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

package org.opendc.web.proto.user;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.opendc.web.proto.OperationalPhenomena;
import org.opendc.web.proto.Workload;

/**
 * A single scenario to be explored by the simulator.
 */
public record Scenario(
        long id,
        int number,
        Project project,
        Portfolio.Summary portfolio,
        String name,
        Workload workload,
        Topology.Summary topology,
        OperationalPhenomena phenomena,
        String schedulerName,
        List<Job> jobs) {
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
    public record Create(
            @NotBlank(message = "Name must not be empty") String name,
            Workload.Spec workload,
            long topology,
            OperationalPhenomena phenomena,
            String schedulerName) {}

    /**
     * A summary view of a [Scenario] provided for nested relations.
     *
     * @param id The unique identifier of the scenario.
     * @param number The number of the scenario for the project.
     * @param name The name of the scenario.
     * @param workload The workload to be modeled by the scenario.
     * @param phenomena The phenomena simulated for this scenario.
     * @param schedulerName The scheduler name to use for the experiment.
     * @param jobs The simulation jobs associated with the scenario.
     */
    @Schema(name = "Scenario.Summary")
    public record Summary(
            long id,
            int number,
            String name,
            Workload workload,
            Topology.Summary topology,
            OperationalPhenomena phenomena,
            String schedulerName,
            List<Job> jobs) {}
}
