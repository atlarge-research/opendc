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

package org.opendc.web.server.rest.user;

import org.opendc.web.server.model.Experiment;
import org.opendc.web.server.model.Job;
import org.opendc.web.server.model.MachinePrefab;
import org.opendc.web.server.model.Project;
import org.opendc.web.server.model.ProjectAuthorization;
import org.opendc.web.server.model.RackPrefab;
import org.opendc.web.server.model.Scenario;
import org.opendc.web.server.model.Topology;
import org.opendc.web.server.rest.BaseProtocol;

/**
 * DTO-conversions for the user protocol.
 */
public final class UserProtocol {
    /**
     * Private constructor to prevent instantiation of class.
     */
    private UserProtocol() {}

    /**
     * Convert a {@link ProjectAuthorization} entity into a {@link Project} DTO.
     */
    public static org.opendc.web.proto.user.Project toDto(ProjectAuthorization auth) {
        Project project = auth.project;
        return new org.opendc.web.proto.user.Project(
                project.id, project.name, project.createdAt, project.updatedAt, auth.role);
    }

    /**
     * Convert a {@link Experiment} entity into a {@link org.opendc.web.proto.user.Experiment} DTO.
     */
    public static org.opendc.web.proto.user.Experiment toDto(Experiment experiment, ProjectAuthorization auth) {
        return new org.opendc.web.proto.user.Experiment(
                experiment.id,
                experiment.number,
                toDto(auth),
                experiment.name,
                experiment.targets,
                experiment.scenarios.stream().map(UserProtocol::toSummaryDto).toList());
    }

    /**
     * Convert a {@link Experiment} entity into a {@link org.opendc.web.proto.user.Experiment.Summary} DTO.
     */
    public static org.opendc.web.proto.user.Experiment.Summary toSummaryDto(Experiment experiment) {
        return new org.opendc.web.proto.user.Experiment.Summary(
                experiment.id, experiment.number, experiment.name, experiment.targets);
    }

    /**
     * Convert a {@link Topology} entity into a {@link org.opendc.web.proto.user.Topology} DTO.
     */
    public static org.opendc.web.proto.user.Topology toDto(Topology topology, ProjectAuthorization auth) {
        return new org.opendc.web.proto.user.Topology(
                topology.id,
                topology.number,
                toDto(auth),
                topology.name,
                topology.datacenters,
                topology.createdAt,
                topology.updatedAt);
    }

    /**
     * Convert a {@link Topology} entity into a {@link org.opendc.web.proto.user.Topology.Summary} DTO.
     */
    public static org.opendc.web.proto.user.Topology.Summary toSummaryDto(Topology topology) {
        return new org.opendc.web.proto.user.Topology.Summary(
                topology.id, topology.number, topology.name, topology.createdAt, topology.updatedAt);
    }

    /**
     * Convert a {@link RackPrefab} entity into a {@link org.opendc.web.proto.user.RackPrefab} DTO.
     */
    public static org.opendc.web.proto.user.RackPrefab toDto(RackPrefab rackPrefab, ProjectAuthorization auth) {
        return new org.opendc.web.proto.user.RackPrefab(
                rackPrefab.id,
                rackPrefab.number,
                toDto(auth),
                rackPrefab.name,
                rackPrefab.rack,
                rackPrefab.createdAt,
                rackPrefab.updatedAt);
    }

    /**
     * Convert a {@link MachinePrefab} entity into a {@link org.opendc.web.proto.user.MachinePrefab} DTO.
     */
    public static org.opendc.web.proto.user.MachinePrefab toDto(
            MachinePrefab machinePrefab, ProjectAuthorization auth) {
        return new org.opendc.web.proto.user.MachinePrefab(
                machinePrefab.id,
                machinePrefab.number,
                toDto(auth),
                machinePrefab.name,
                machinePrefab.machine,
                machinePrefab.createdAt,
                machinePrefab.updatedAt);
    }

    /**
     * Convert a {@link Scenario} entity into a {@link org.opendc.web.proto.user.Scenario} DTO.
     */
    public static org.opendc.web.proto.user.Scenario toDto(Scenario scenario, ProjectAuthorization auth) {
        return new org.opendc.web.proto.user.Scenario(
                scenario.id,
                scenario.number,
                toDto(auth),
                toSummaryDto(scenario.experiment),
                scenario.name,
                BaseProtocol.toDto(scenario.workload),
                toSummaryDto(scenario.topology),
                scenario.phenomena,
                scenario.schedulerName,
                scenario.jobs.stream().map(UserProtocol::toDto).toList(),
                scenario.exportModels);
    }

    /**
     * Convert a {@link Scenario} entity into a {@link org.opendc.web.proto.user.Scenario.Summary} DTO.
     */
    public static org.opendc.web.proto.user.Scenario.Summary toSummaryDto(Scenario scenario) {
        return new org.opendc.web.proto.user.Scenario.Summary(
                scenario.id,
                scenario.number,
                scenario.name,
                BaseProtocol.toDto(scenario.workload),
                toSummaryDto(scenario.topology),
                scenario.phenomena,
                scenario.schedulerName,
                scenario.jobs.stream().map(UserProtocol::toDto).toList());
    }

    /**
     * Convert a {@link Job} entity into a {@link org.opendc.web.proto.user.Job} DTO.
     */
    public static org.opendc.web.proto.user.Job toDto(Job job) {
        return new org.opendc.web.proto.user.Job(
                job.id, job.state, job.createdAt, job.updatedAt, job.results, job.hasExports);
    }
}
