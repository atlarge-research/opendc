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

package org.opendc.web.api.rest.user

import io.quarkus.security.identity.SecurityIdentity
import org.opendc.web.api.service.PortfolioService
import org.opendc.web.proto.user.Portfolio
import javax.annotation.security.RolesAllowed
import javax.inject.Inject
import javax.transaction.Transactional
import javax.validation.Valid
import javax.ws.rs.*

/**
 * A resource representing the portfolios of a project.
 */
@Path("/projects/{project}/portfolios")
@RolesAllowed("openid")
class PortfolioResource @Inject constructor(
    private val portfolioService: PortfolioService,
    private val identity: SecurityIdentity,
) {
    /**
     * Get all portfolios that belong to the specified project.
     */
    @GET
    fun getAll(@PathParam("project") projectId: Long): List<Portfolio> {
        return portfolioService.findAll(identity.principal.name, projectId)
    }

    /**
     * Create a portfolio for this project.
     */
    @POST
    @Transactional
    fun create(@PathParam("project") projectId: Long, @Valid request: Portfolio.Create): Portfolio {
        return portfolioService.create(identity.principal.name, projectId, request) ?: throw WebApplicationException("Project not found", 404)
    }

    /**
     * Obtain a portfolio by its identifier.
     */
    @GET
    @Path("{portfolio}")
    fun get(@PathParam("project") projectId: Long, @PathParam("portfolio") number: Int): Portfolio {
        return portfolioService.findOne(identity.principal.name, projectId, number) ?: throw WebApplicationException("Portfolio not found", 404)
    }

    /**
     * Delete a portfolio.
     */
    @DELETE
    @Path("{portfolio}")
    fun delete(@PathParam("project") projectId: Long, @PathParam("portfolio") number: Int): Portfolio {
        return portfolioService.delete(identity.principal.name, projectId, number) ?: throw WebApplicationException("Portfolio not found", 404)
    }
}
