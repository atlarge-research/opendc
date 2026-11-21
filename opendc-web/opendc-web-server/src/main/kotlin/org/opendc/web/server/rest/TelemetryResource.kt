/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.web.server.rest

import io.quarkus.security.identity.SecurityIdentity
import jakarta.annotation.security.RolesAllowed
import jakarta.transaction.Transactional
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.opendc.web.launcher.TelemetryReport
import org.opendc.web.server.auth.EXECUTION_ATTRIBUTE
import org.opendc.web.server.auth.Roles
import org.opendc.web.server.model.Execution
import org.opendc.web.server.model.ExecutionUnit
import org.opendc.web.server.model.UnitState
import org.opendc.web.server.telemetry.RunKey
import org.opendc.web.server.telemetry.TelemetryStore
import java.util.UUID

/**
 * Where launchers say how far they have got. The caller is an execution token, not a user, and it can
 * only write progress into the work it was handed. Nothing reported here decides anything, so the
 * numbers are only ever overwritten, never accumulated.
 */
@Path("telemetry")
@RolesAllowed(Roles.EXECUTION)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class TelemetryResource(
    private val store: TelemetryStore,
    private val caller: SecurityIdentity,
) {
    @POST
    @Transactional
    fun report(report: TelemetryReport): Response {
        val id = caller.getAttribute<UUID>(EXECUTION_ATTRIBUTE) ?: throw notAuthenticated()
        val execution = Execution.findByPublicId(id) ?: throw notAuthenticated()
        if (execution.state.isTerminal) {
            throw conflict("This execution has already finished")
        }
        val experimentId = execution.experiment.publicId
        val carried =
            ExecutionUnit
                .findByExecution(execution.id)
                .map { it.unit }
                .filter { it.state == UnitState.CARRIED }
                .associateBy { it.scenarioIndex to it.seed }
        for (run in report.runs) {
            // Work this execution is not carrying, such as work cancelled while it was out, is dropped
            // rather than refused, so the runs it got right are kept.
            val unit = carried[run.scenarioIndex to run.seed] ?: continue
            unit.completedTasks = run.completedTasks.coerceIn(0, unit.totalTasks)
            store.write(RunKey(experimentId, run.scenarioIndex, run.seed), run.series)
        }
        return Response.status(204).build()
    }
}
