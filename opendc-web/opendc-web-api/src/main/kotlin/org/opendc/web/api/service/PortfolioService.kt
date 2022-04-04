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
import org.opendc.web.api.repository.PortfolioRepository
import org.opendc.web.api.repository.ProjectRepository
import org.opendc.web.proto.user.Portfolio
import java.time.Instant
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import org.opendc.web.api.model.Portfolio as PortfolioEntity

/**
 * Service for managing [Portfolio]s.
 */
@ApplicationScoped
class PortfolioService @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val portfolioRepository: PortfolioRepository
) {
    /**
     * List all [Portfolio]s that belong a certain project.
     */
    fun findAll(userId: String, projectId: Long): List<Portfolio> {
        // User must have access to project
        val auth = projectRepository.findOne(userId, projectId) ?: return emptyList()
        val project = auth.toUserDto()
        return portfolioRepository.findAll(projectId).map { it.toUserDto(project) }
    }

    /**
     * Find a [Portfolio] with the specified [number] belonging to [project][projectId].
     */
    fun findOne(userId: String, projectId: Long, number: Int): Portfolio? {
        // User must have access to project
        val auth = projectRepository.findOne(userId, projectId) ?: return null
        return portfolioRepository.findOne(projectId, number)?.toUserDto(auth.toUserDto())
    }

    /**
     * Delete the portfolio with the specified [number] belonging to [project][projectId].
     */
    fun delete(userId: String, projectId: Long, number: Int): Portfolio? {
        // User must have access to project
        val auth = projectRepository.findOne(userId, projectId)

        if (auth == null) {
            return null
        } else if (!auth.role.canEdit) {
            throw IllegalStateException("Not permitted to edit project")
        }

        val entity = portfolioRepository.findOne(projectId, number) ?: return null
        val portfolio = entity.toUserDto(auth.toUserDto())
        portfolioRepository.delete(entity)
        return portfolio
    }

    /**
     * Construct a new [Portfolio] with the specified name.
     */
    fun create(userId: String, projectId: Long, request: Portfolio.Create): Portfolio? {
        // User must have access to project
        val auth = projectRepository.findOne(userId, projectId)

        if (auth == null) {
            return null
        } else if (!auth.role.canEdit) {
            throw IllegalStateException("Not permitted to edit project")
        }

        val now = Instant.now()
        val project = auth.project
        val number = projectRepository.allocatePortfolio(auth.project, now)

        val portfolio = PortfolioEntity(0, number, request.name, project, request.targets)

        project.portfolios.add(portfolio)
        portfolioRepository.save(portfolio)

        return portfolio.toUserDto(auth.toUserDto())
    }
}
