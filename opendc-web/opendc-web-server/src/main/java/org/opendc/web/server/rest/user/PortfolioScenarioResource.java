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
import java.time.Instant;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import org.opendc.web.proto.JobState;
import org.opendc.web.server.model.Job;
import org.opendc.web.server.model.Portfolio;
import org.opendc.web.server.model.ProjectAuthorization;
import org.opendc.web.server.model.Scenario;
import org.opendc.web.server.model.Topology;
import org.opendc.web.server.model.Trace;
import org.opendc.web.server.model.Workload;
import org.opendc.web.server.service.UserAccountingService;

/**
 * A resource representing the scenarios of a portfolio.
 */
@Path("/projects/{project}/portfolios/{portfolio}/scenarios")
@RolesAllowed("openid")
@Produces("application/json")
public final class PortfolioScenarioResource {
    /**
     * The service for managing the user accounting.
     */
    private final UserAccountingService accountingService;

    /**
     * The identity of the current user.
     */
    private final SecurityIdentity identity;

    /**
     * Construct a {@link PortfolioScenarioResource}.
     *
     * @param accountingService The {@link UserAccountingService} instance to use.
     * @param identity The {@link SecurityIdentity} of the current user.
     */
    public PortfolioScenarioResource(UserAccountingService accountingService, SecurityIdentity identity) {
        this.accountingService = accountingService;
        this.identity = identity;
    }

    /**
     * Get all scenarios that belong to the specified portfolio.
     */
    @GET
    public List<org.opendc.web.proto.user.Scenario> get(
            @PathParam("project") long projectId, @PathParam("portfolio") int portfolioNumber) {
        // User must have access to project
        ProjectAuthorization auth =
                ProjectAuthorization.findByUser(identity.getPrincipal().getName(), projectId);

        if (auth == null) {
            return List.of();
        }

        return org.opendc.web.server.model.Scenario.findByPortfolio(projectId, portfolioNumber).list().stream()
                .map((s) -> UserProtocol.toDto(s, auth))
                .toList();
    }

    /**
     * Create a scenario for this portfolio.
     */
    @POST
    @Transactional
    @Consumes("application/json")
    public org.opendc.web.proto.user.Scenario create(
            @PathParam("project") long projectId,
            @PathParam("portfolio") int portfolioNumber,
            @Valid org.opendc.web.proto.user.Scenario.Create request) {
        // User must have access to project
        String userId = identity.getPrincipal().getName();
        ProjectAuthorization auth = ProjectAuthorization.findByUser(userId, projectId);

        if (auth == null) {
            throw new WebApplicationException("Portfolio not found", 404);
        } else if (!auth.canEdit()) {
            throw new WebApplicationException("Not permitted to edit project", 403);
        }

        Portfolio portfolio = Portfolio.findByProject(projectId, portfolioNumber);

        if (portfolio == null) {
            throw new WebApplicationException("Portfolio not found", 404);
        }

        Topology topology = Topology.findByProject(projectId, (int) request.getTopology());
        if (topology == null) {
            throw new WebApplicationException("Referred topology does not exist", 400);
        }

        Trace trace = Trace.findById(request.getWorkload().getTrace());
        if (trace == null) {
            throw new WebApplicationException("Referred trace does not exist", 400);
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
        scenario.persist();

        Job job = new Job(scenario, userId, now, portfolio.targets.getRepeats());
        job.persist();

        // Fail the job if there is not enough budget for the simulation
        if (!accountingService.hasSimulationBudget(userId)) {
            job.state = JobState.FAILED;
        }

        scenario.jobs.add(job);
        portfolio.scenarios.add(scenario);

        return UserProtocol.toDto(scenario, auth);
    }
}
