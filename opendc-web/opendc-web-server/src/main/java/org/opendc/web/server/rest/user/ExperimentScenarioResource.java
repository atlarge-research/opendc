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

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import java.time.Instant;
import java.util.List;
import org.opendc.web.proto.JobState;
import org.opendc.web.server.model.Experiment;
import org.opendc.web.server.model.Job;
import org.opendc.web.server.model.ProjectAuthorization;
import org.opendc.web.server.model.Scenario;
import org.opendc.web.server.model.Topology;
import org.opendc.web.server.model.Trace;
import org.opendc.web.server.model.Workload;
import org.opendc.web.server.service.UserAccountingService;

/**
 * A resource representing the scenarios of an experiment.
 */
@Path("/projects/{project}/experiments/{experiment}/scenarios")
@RolesAllowed("openid")
@Produces("application/json")
public final class ExperimentScenarioResource {
    /**
     * The service for managing the user accounting.
     */
    private final UserAccountingService accountingService;

    /**
     * The identity of the current user.
     */
    private final SecurityIdentity identity;

    /**
     * Construct a {@link ExperimentScenarioResource}.
     *
     * @param accountingService The {@link UserAccountingService} instance to use.
     * @param identity The {@link SecurityIdentity} of the current user.
     */
    public ExperimentScenarioResource(UserAccountingService accountingService, SecurityIdentity identity) {
        this.accountingService = accountingService;
        this.identity = identity;
    }

    /**
     * Get all scenarios that belong to the specified experiment.
     */
    @GET
    public List<org.opendc.web.proto.user.Scenario> get(
            @PathParam("project") long projectId, @PathParam("experiment") int experimentNumber) {
        // User must have access to project
        ProjectAuthorization auth =
                ProjectAuthorization.findByUser(identity.getPrincipal().getName(), projectId);

        if (auth == null) {
            return List.of();
        }

        return org.opendc.web.server.model.Scenario.findByExperiment(projectId, experimentNumber).list().stream()
                .map((s) -> UserProtocol.toDto(s, auth))
                .toList();
    }

    /**
     * Create a scenario for this experiment.
     */
    @POST
    @Transactional
    @Consumes("application/json")
    public org.opendc.web.proto.user.Scenario create(
            @PathParam("project") long projectId,
            @PathParam("experiment") int experimentNumber,
            @Valid org.opendc.web.proto.user.Scenario.Create request) {
        // User must have access to project
        String userId = identity.getPrincipal().getName();
        ProjectAuthorization auth = ProjectAuthorization.findByUser(userId, projectId);

        if (auth == null) {
            throw new WebApplicationException("Experiment not found", 404);
        } else if (!auth.canEdit()) {
            throw new WebApplicationException("Not permitted to edit project", 403);
        }

        Experiment experiment = Experiment.findByProject(projectId, experimentNumber);

        if (experiment == null) {
            throw new WebApplicationException("Experiment not found", 404);
        }

        Topology topology = Topology.findByProject(projectId, (int) request.topology());
        if (topology == null) {
            throw new WebApplicationException("Referred topology does not exist", 400);
        }

        Trace trace = Trace.findById(request.workload().trace());
        if (trace == null) {
            throw new WebApplicationException("Referred trace does not exist", 400);
        }

        var now = Instant.now();
        var project = auth.project;
        int number = project.allocateScenario(now);

        Scenario scenario = new Scenario(
                project,
                experiment,
                number,
                request.name(),
                new Workload(trace, request.workload().samplingFraction()),
                topology,
                request.phenomena(),
                request.schedulerName(),
                request.exportModels());
        scenario.persist();

        Job job = new Job(scenario, userId, now, experiment.targets.repeats());
        job.persist();

        // Fail the job if there is not enough budget for the simulation
        if (!accountingService.hasSimulationBudget(userId)) {
            job.state = JobState.FAILED;
        }

        scenario.jobs.add(job);
        experiment.scenarios.add(scenario);

        return UserProtocol.toDto(scenario, auth);
    }
}
