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

package org.opendc.web.api.service

import org.opendc.web.api.model.Job
import org.opendc.web.api.model.Portfolio
import org.opendc.web.api.model.Scenario
import org.opendc.web.api.model.Topology

/**
 * Conversions into DTOs provided to OpenDC runners.
 */

/**
 * Convert a [Topology] into a runner-facing DTO.
 */
internal fun Topology.toRunnerDto(): org.opendc.web.proto.runner.Topology {
    return org.opendc.web.proto.runner.Topology(id, number, name, rooms, createdAt, updatedAt)
}

/**
 * Convert a [Portfolio] into a runner-facing DTO.
 */
internal fun Portfolio.toRunnerDto(): org.opendc.web.proto.runner.Portfolio {
    return org.opendc.web.proto.runner.Portfolio(id, number, name, targets)
}

/**
 * Convert a [Job] into a runner-facing DTO.
 */
internal fun Job.toRunnerDto(): org.opendc.web.proto.runner.Job {
    return org.opendc.web.proto.runner.Job(id, scenario.toRunnerDto(), state, createdAt, updatedAt, results)
}

/**
 * Convert a [Job] into a runner-facing DTO.
 */
internal fun Scenario.toRunnerDto(): org.opendc.web.proto.runner.Scenario {
    return org.opendc.web.proto.runner.Scenario(
        id,
        number,
        portfolio.toRunnerDto(),
        name,
        workload.toDto(),
        topology.toRunnerDto(),
        phenomena,
        schedulerName
    )
}
