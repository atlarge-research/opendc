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
import javax.ws.rs.*

/**
 * A resource representing the scenarios of a portfolio.
 */
@Path("/projects/{project}/scenarios")
@RolesAllowed("openid")
class ScenarioResource @Inject constructor(
    private val scenarioService: ScenarioService,
    private val identity: SecurityIdentity
) {
    /**
     * Obtain a scenario by its identifier.
     */
    @GET
    @Path("{scenario}")
    fun get(@PathParam("project") projectId: Long, @PathParam("scenario") number: Int): Scenario {
        return scenarioService.findOne(identity.principal.name, projectId, number) ?: throw WebApplicationException("Scenario not found", 404)
    }

    /**
     * Delete a scenario.
     */
    @DELETE
    @Path("{scenario}")
    @Transactional
    fun delete(@PathParam("project") projectId: Long, @PathParam("scenario") number: Int): Scenario {
        return scenarioService.delete(identity.principal.name, projectId, number) ?: throw WebApplicationException("Scenario not found", 404)
    }
}
