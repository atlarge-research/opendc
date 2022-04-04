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

import org.opendc.web.api.model.*
import org.opendc.web.proto.user.Project

/**
 * Conversions into DTOs provided to users.
 */

/**
 * Convert a [Trace] entity into a [org.opendc.web.proto.Trace] DTO.
 */
internal fun Trace.toUserDto(): org.opendc.web.proto.Trace {
    return org.opendc.web.proto.Trace(id, name, type)
}

/**
 * Convert a [ProjectAuthorization] entity into a [Project] DTO.
 */
internal fun ProjectAuthorization.toUserDto(): Project {
    return Project(project.id, project.name, project.createdAt, project.updatedAt, role)
}

/**
 * Convert a [Topology] entity into a [org.opendc.web.proto.user.Topology] DTO.
 */
internal fun Topology.toUserDto(project: Project): org.opendc.web.proto.user.Topology {
    return org.opendc.web.proto.user.Topology(id, number, project, name, rooms, createdAt, updatedAt)
}

/**
 * Convert a [Topology] entity into a [org.opendc.web.proto.user.Topology.Summary] DTO.
 */
private fun Topology.toSummaryDto(): org.opendc.web.proto.user.Topology.Summary {
    return org.opendc.web.proto.user.Topology.Summary(id, number, name, createdAt, updatedAt)
}

/**
 * Convert a [Portfolio] entity into a [org.opendc.web.proto.user.Portfolio] DTO.
 */
internal fun Portfolio.toUserDto(project: Project): org.opendc.web.proto.user.Portfolio {
    return org.opendc.web.proto.user.Portfolio(id, number, project, name, targets, scenarios.map { it.toSummaryDto() })
}

/**
 * Convert a [Portfolio] entity into a [org.opendc.web.proto.user.Portfolio.Summary] DTO.
 */
private fun Portfolio.toSummaryDto(): org.opendc.web.proto.user.Portfolio.Summary {
    return org.opendc.web.proto.user.Portfolio.Summary(id, number, name, targets)
}

/**
 * Convert a [Scenario] entity into a [org.opendc.web.proto.user.Scenario] DTO.
 */
internal fun Scenario.toUserDto(project: Project): org.opendc.web.proto.user.Scenario {
    return org.opendc.web.proto.user.Scenario(
        id,
        number,
        project,
        portfolio.toSummaryDto(),
        name,
        workload.toDto(),
        topology.toSummaryDto(),
        phenomena,
        schedulerName,
        job.toUserDto()
    )
}

/**
 * Convert a [Scenario] entity into a [org.opendc.web.proto.user.Scenario.Summary] DTO.
 */
private fun Scenario.toSummaryDto(): org.opendc.web.proto.user.Scenario.Summary {
    return org.opendc.web.proto.user.Scenario.Summary(
        id,
        number,
        name,
        workload.toDto(),
        topology.toSummaryDto(),
        phenomena,
        schedulerName,
        job.toUserDto()
    )
}

/**
 * Convert a [Job] entity into a [org.opendc.web.proto.user.Job] DTO.
 */
internal fun Job.toUserDto(): org.opendc.web.proto.user.Job {
    return org.opendc.web.proto.user.Job(id, state, createdAt, updatedAt, results)
}

/**
 * Convert a [Workload] entity into a DTO.
 */
internal fun Workload.toDto(): org.opendc.web.proto.Workload {
    return org.opendc.web.proto.Workload(trace.toUserDto(), samplingFraction)
}
