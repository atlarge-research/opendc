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

package org.opendc.web.server.service;

import java.time.Instant;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import org.opendc.web.proto.JobState;
import org.opendc.web.server.model.Job;
import org.opendc.web.server.model.Portfolio;
import org.opendc.web.server.model.ProjectAuthorization;
import org.opendc.web.server.model.Scenario;
import org.opendc.web.server.model.Topology;
import org.opendc.web.server.model.Trace;
import org.opendc.web.server.model.Workload;

/**
 * Service for managing {@link Scenario}s.
 */
@ApplicationScoped
public final class ScenarioService {
    /**
     * The service for managing the user accounting.
     */
    private final UserAccountingService accountingService;

    /**
     * Construct a {@link ScenarioService} instance.
     *
     * @param accountingService The {@link UserAccountingService} instance to use.
     */
    public ScenarioService(UserAccountingService accountingService) {
        this.accountingService = accountingService;
    }

    /**
     * List all {@link Scenario}s that belong a certain portfolio.
     */
    public List<org.opendc.web.proto.user.Scenario> findAll(String userId, long projectId, int number) {
        // User must have access to project
        ProjectAuthorization auth = ProjectAuthorization.findByUser(userId, projectId);

        if (auth == null) {
            return List.of();
        }

        return Scenario.findByPortfolio(projectId, number).stream()
                .map((s) -> toUserDto(s, auth))
                .toList();
    }

    /**
     * Obtain a {@link Scenario} by identifier.
     */
    public org.opendc.web.proto.user.Scenario findOne(String userId, long projectId, int number) {
        // User must have access to project
        ProjectAuthorization auth = ProjectAuthorization.findByUser(userId, projectId);

        if (auth == null) {
            return null;
        }

        Scenario scenario = Scenario.findByProject(projectId, number);

        if (scenario == null) {
            return null;
        }

        return toUserDto(scenario, auth);
    }

    /**
     * Delete the specified scenario.
     */
    public org.opendc.web.proto.user.Scenario delete(String userId, long projectId, int number) {
        // User must have access to project
        ProjectAuthorization auth = ProjectAuthorization.findByUser(userId, projectId);

        if (auth == null) {
            return null;
        } else if (!auth.canEdit()) {
            throw new IllegalStateException("Not permitted to edit project");
        }

        Scenario entity = Scenario.findByProject(projectId, number);
        if (entity == null) {
            return null;
        }

        entity.delete();
        return toUserDto(entity, auth);
    }

    /**
     * Construct a new {@link Scenario} with the specified data.
     */
    public org.opendc.web.proto.user.Scenario create(
            String userId, long projectId, int portfolioNumber, org.opendc.web.proto.user.Scenario.Create request) {
        // User must have access to project
        ProjectAuthorization auth = ProjectAuthorization.findByUser(userId, projectId);

        if (auth == null) {
            return null;
        } else if (!auth.canEdit()) {
            throw new IllegalStateException("Not permitted to edit project");
        }

        Portfolio portfolio = Portfolio.findByProject(projectId, portfolioNumber);

        if (portfolio == null) {
            return null;
        }

        Topology topology = Topology.findByProject(projectId, (int) request.getTopology());
        if (topology == null) {
            throw new IllegalArgumentException("Referred topology does not exist");
        }

        Trace trace = Trace.findById(request.getWorkload().getTrace());
        if (trace == null) {
            throw new IllegalArgumentException("Referred trace does not exist");
        }

        var now = Instant.now();
        var project = auth.project;
        int number = project.allocateScenario(now);

        Scenario scenario = new Scenario(
                project,
                portfolio,
                number,
                request.getName(),
                new Workload(trace, request.getWorkload().getSamplingFraction()),
                topology,
                request.getPhenomena(),
                request.getSchedulerName());
        Job job = new Job(scenario, userId, now, portfolio.targets.getRepeats());

        // Fail the job if there is not enough budget for the simulation
        if (!accountingService.hasSimulationBudget(userId)) {
            job.state = JobState.FAILED;
        }

        scenario.job = job;
        portfolio.scenarios.add(scenario);
        scenario.persist();

        return toUserDto(scenario, auth);
    }

    /**
     * Convert a {@link Scenario} entity into a {@link org.opendc.web.proto.user.Scenario} DTO.
     */
    public static org.opendc.web.proto.user.Scenario toUserDto(Scenario scenario, ProjectAuthorization auth) {
        return new org.opendc.web.proto.user.Scenario(
                scenario.id,
                scenario.number,
                ProjectService.toUserDto(auth),
                PortfolioService.toSummaryDto(scenario.portfolio),
                scenario.name,
                toDto(scenario.workload),
                TopologyService.toSummaryDto(scenario.topology),
                scenario.phenomena,
                scenario.schedulerName,
                JobService.toUserDto(scenario.job));
    }

    /**
     * Convert a {@link Scenario} entity into a {@link org.opendc.web.proto.user.Scenario.Summary} DTO.
     */
    public static org.opendc.web.proto.user.Scenario.Summary toSummaryDto(Scenario scenario) {
        return new org.opendc.web.proto.user.Scenario.Summary(
                scenario.id,
                scenario.number,
                scenario.name,
                toDto(scenario.workload),
                TopologyService.toSummaryDto(scenario.topology),
                scenario.phenomena,
                scenario.schedulerName,
                JobService.toUserDto(scenario.job));
    }

    /**
     * Convert a {@link Scenario} into a runner-facing DTO.
     */
    public static org.opendc.web.proto.runner.Scenario toRunnerDto(Scenario scenario) {
        return new org.opendc.web.proto.runner.Scenario(
                scenario.id,
                scenario.number,
                PortfolioService.toRunnerDto(scenario.portfolio),
                scenario.name,
                toDto(scenario.workload),
                TopologyService.toRunnerDto(scenario.topology),
                scenario.phenomena,
                scenario.schedulerName);
    }

    /**
     * Convert a {@link Workload} entity into a DTO.
     */
    public static org.opendc.web.proto.Workload toDto(Workload workload) {
        return new org.opendc.web.proto.Workload(TraceService.toUserDto(workload.trace), workload.samplingFraction);
    }
}
