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
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import java.time.Instant;
import java.util.List;
import org.opendc.web.server.model.Portfolio;
import org.opendc.web.server.model.ProjectAuthorization;

/**
 * A resource representing the portfolios of a project.
 */
@Produces("application/json")
@Path("/projects/{project}/portfolios")
@RolesAllowed("openid")
public final class PortfolioResource {
    /**
     * The identity of the current user.
     */
    private final SecurityIdentity identity;

    /**
     * Construct a {@link PortfolioResource}.
     *
     * @param identity The {@link SecurityIdentity} of the current user.
     */
    public PortfolioResource(SecurityIdentity identity) {
        this.identity = identity;
    }

    /**
     * Get all portfolios that belong to the specified project.
     */
    @GET
    public List<org.opendc.web.proto.user.Portfolio> getAll(@PathParam("project") long projectId) {
        // User must have access to project
        ProjectAuthorization auth =
                ProjectAuthorization.findByUser(identity.getPrincipal().getName(), projectId);

        if (auth == null) {
            return List.of();
        }

        return Portfolio.findByProject(projectId).list().stream()
                .map((p) -> UserProtocol.toDto(p, auth))
                .toList();
    }

    /**
     * Create a portfolio for this project.
     */
    @POST
    @Transactional
    @Consumes("application/json")
    public org.opendc.web.proto.user.Portfolio create(
            @PathParam("project") long projectId, @Valid org.opendc.web.proto.user.Portfolio.Create request) {
        // User must have access to project
        ProjectAuthorization auth =
                ProjectAuthorization.findByUser(identity.getPrincipal().getName(), projectId);

        if (auth == null) {
            throw new WebApplicationException("Project not found", 404);
        } else if (!auth.canEdit()) {
            throw new WebApplicationException("Not permitted to edit project", 403);
        }

        var now = Instant.now();
        var project = auth.project;
        int number = project.allocatePortfolio(now);

        Portfolio portfolio = new Portfolio(project, number, request.name(), request.targets());

        project.portfolios.add(portfolio);
        portfolio.persist();

        return UserProtocol.toDto(portfolio, auth);
    }

    /**
     * Obtain a portfolio by its identifier.
     */
    @GET
    @Path("{portfolio}")
    public org.opendc.web.proto.user.Portfolio get(
            @PathParam("project") long projectId, @PathParam("portfolio") int number) {
        // User must have access to project
        ProjectAuthorization auth =
                ProjectAuthorization.findByUser(identity.getPrincipal().getName(), projectId);

        if (auth == null) {
            throw new WebApplicationException("Portfolio not found", 404);
        }

        Portfolio portfolio = Portfolio.findByProject(projectId, number);

        if (portfolio == null) {
            throw new WebApplicationException("Portfolio not found", 404);
        }

        return UserProtocol.toDto(portfolio, auth);
    }

    /**
     * Delete a portfolio.
     */
    @DELETE
    @Path("{portfolio}")
    @Transactional
    public org.opendc.web.proto.user.Portfolio delete(
            @PathParam("project") long projectId, @PathParam("portfolio") int number) {
        // User must have access to project
        ProjectAuthorization auth =
                ProjectAuthorization.findByUser(identity.getPrincipal().getName(), projectId);

        if (auth == null) {
            throw new WebApplicationException("Portfolio not found", 404);
        } else if (!auth.canEdit()) {
            throw new WebApplicationException("Not permitted to edit project", 403);
        }

        Portfolio entity = Portfolio.findByProject(projectId, number);
        if (entity == null) {
            throw new WebApplicationException("Portfolio not found", 404);
        }

        entity.delete();
        return UserProtocol.toDto(entity, auth);
    }
}
