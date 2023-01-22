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
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import org.opendc.web.server.service.PortfolioService;

/**
 * A resource representing the portfolios of a project.
 */
@Produces("application/json")
@Path("/projects/{project}/portfolios")
@RolesAllowed("openid")
public final class PortfolioResource {
    /**
     * The service for managing the user portfolios.
     */
    private final PortfolioService portfolioService;

    /**
     * The identity of the current user.
     */
    private final SecurityIdentity identity;

    /**
     * Construct a {@link PortfolioResource}.
     *
     * @param portfolioService The {@link PortfolioService} instance to use.
     * @param identity The {@link SecurityIdentity} of the current user.
     */
    public PortfolioResource(PortfolioService portfolioService, SecurityIdentity identity) {
        this.portfolioService = portfolioService;
        this.identity = identity;
    }

    /**
     * Get all portfolios that belong to the specified project.
     */
    @GET
    public List<org.opendc.web.proto.user.Portfolio> getAll(@PathParam("project") long projectId) {
        return portfolioService.findByUser(identity.getPrincipal().getName(), projectId);
    }

    /**
     * Create a portfolio for this project.
     */
    @POST
    @Transactional
    public org.opendc.web.proto.user.Portfolio create(
            @PathParam("project") long projectId, @Valid org.opendc.web.proto.user.Portfolio.Create request) {
        var portfolio = portfolioService.create(identity.getPrincipal().getName(), projectId, request);
        if (portfolio == null) {
            throw new WebApplicationException("Project not found", 404);
        }

        return portfolio;
    }

    /**
     * Obtain a portfolio by its identifier.
     */
    @GET
    @Path("{portfolio}")
    public org.opendc.web.proto.user.Portfolio get(
            @PathParam("project") long projectId, @PathParam("portfolio") int number) {
        var portfolio = portfolioService.findByUser(identity.getPrincipal().getName(), projectId, number);
        if (portfolio == null) {
            throw new WebApplicationException("Portfolio not found", 404);
        }

        return portfolio;
    }

    /**
     * Delete a portfolio.
     */
    @DELETE
    @Path("{portfolio}")
    @Transactional
    public org.opendc.web.proto.user.Portfolio delete(
            @PathParam("project") long projectId, @PathParam("portfolio") int number) {
        var portfolio = portfolioService.delete(identity.getPrincipal().getName(), projectId, number);
        if (portfolio == null) {
            throw new WebApplicationException("Portfolio not found", 404);
        }

        return portfolio;
    }
}
