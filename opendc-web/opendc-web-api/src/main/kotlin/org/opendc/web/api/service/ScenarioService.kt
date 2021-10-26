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

package org.opendc.web.api.service

import org.opendc.web.api.model.*
import org.opendc.web.api.repository.*
import org.opendc.web.proto.user.Scenario
import java.time.Instant
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Service for managing [Scenario]s.
 */
@ApplicationScoped
class ScenarioService @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val portfolioRepository: PortfolioRepository,
    private val topologyRepository: TopologyRepository,
    private val traceRepository: TraceRepository,
    private val scenarioRepository: ScenarioRepository,
) {
    /**
     * List all [Scenario]s that belong a certain portfolio.
     */
    fun findAll(userId: String, projectId: Long, number: Int): List<Scenario> {
        // User must have access to project
        val auth = projectRepository.findOne(userId, projectId) ?: return emptyList()
        val project = auth.toUserDto()
        return scenarioRepository.findAll(projectId).map { it.toUserDto(project) }
    }

    /**
     * Obtain a [Scenario] by identifier.
     */
    fun findOne(userId: String, projectId: Long, number: Int): Scenario? {
        // User must have access to project
        val auth = projectRepository.findOne(userId, projectId) ?: return null
        val project = auth.toUserDto()
        return scenarioRepository.findOne(projectId, number)?.toUserDto(project)
    }

    /**
     * Delete the specified scenario.
     */
    fun delete(userId: String, projectId: Long, number: Int): Scenario? {
        // User must have access to project
        val auth = projectRepository.findOne(userId, projectId)

        if (auth == null) {
            return null
        } else if (!auth.role.canEdit) {
            throw IllegalStateException("Not permitted to edit project")
        }

        val entity = scenarioRepository.findOne(projectId, number) ?: return null
        val scenario = entity.toUserDto(auth.toUserDto())
        scenarioRepository.delete(entity)
        return scenario
    }

    /**
     * Construct a new [Scenario] with the specified data.
     */
    fun create(userId: String, projectId: Long, portfolioNumber: Int, request: Scenario.Create): Scenario? {
        // User must have access to project
        val auth = projectRepository.findOne(userId, projectId)

        if (auth == null) {
            return null
        } else if (!auth.role.canEdit) {
            throw IllegalStateException("Not permitted to edit project")
        }

        val portfolio = portfolioRepository.findOne(projectId, portfolioNumber) ?: return null
        val topology = requireNotNull(
            topologyRepository.findOne(
                projectId,
                request.topology.toInt()
            )
        ) { "Referred topology does not exist" }
        val trace =
            requireNotNull(traceRepository.findOne(request.workload.trace)) { "Referred trace does not exist" }

        val now = Instant.now()
        val project = auth.project
        val number = projectRepository.allocateScenario(auth.project, now)

        val scenario = Scenario(
            0,
            number,
            request.name,
            project,
            portfolio,
            Workload(trace, request.workload.samplingFraction),
            topology,
            request.phenomena,
            request.schedulerName
        )
        val job = Job(0, scenario, now, portfolio.targets.repeats)

        scenario.job = job
        portfolio.scenarios.add(scenario)
        scenarioRepository.save(scenario)

        return scenario.toUserDto(auth.toUserDto())
    }
}
