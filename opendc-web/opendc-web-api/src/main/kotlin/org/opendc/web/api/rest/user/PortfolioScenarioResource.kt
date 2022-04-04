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
import org.opendc.web.api.service.ScenarioService
import org.opendc.web.proto.user.Scenario
import javax.annotation.security.RolesAllowed
import javax.inject.Inject
import javax.transaction.Transactional
import javax.validation.Valid
import javax.ws.rs.*

/**
 * A resource representing the scenarios of a portfolio.
 */
@Path("/projects/{project}/portfolios/{portfolio}/scenarios")
@RolesAllowed("openid")
class PortfolioScenarioResource @Inject constructor(
    private val scenarioService: ScenarioService,
    private val identity: SecurityIdentity,
) {
    /**
     * Get all scenarios that belong to the specified portfolio.
     */
    @GET
    fun get(@PathParam("project") projectId: Long, @PathParam("portfolio") portfolioNumber: Int): List<Scenario> {
        return scenarioService.findAll(identity.principal.name, projectId, portfolioNumber)
    }

    /**
     * Create a scenario for this portfolio.
     */
    @POST
    @Transactional
    fun create(@PathParam("project") projectId: Long, @PathParam("portfolio") portfolioNumber: Int, @Valid request: Scenario.Create): Scenario {
        return scenarioService.create(identity.principal.name, projectId, portfolioNumber, request) ?: throw WebApplicationException("Portfolio not found", 404)
    }
}
