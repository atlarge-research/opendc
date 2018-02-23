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

import com.atlarge.opendc.model.odc.platform.scheduler.Scheduler
import com.atlarge.opendc.simulator.Instant
import javax.persistence.Entity

/**
 * An experiment definition for the OpenDC database schema.
 *
 * @property id The identifier of the experiment.
 * @property name The name of the experiment.
 * @property scheduler The scheduler used in the experiment.
 * @property trace The trace used for the simulation.
 * @property path The path of the experiment.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
@Entity
data class Experiment(
    val id: Int,
    val name: String,
    val scheduler: Scheduler,
    val trace: Trace,
    val path: Path
) {
    /**
     * The state of the experiment.
     */
    var state: ExperimentState = ExperimentState.QUEUED

    /**
     * The last tick that has been simulated.
     */
    var last: Instant = 0
}
