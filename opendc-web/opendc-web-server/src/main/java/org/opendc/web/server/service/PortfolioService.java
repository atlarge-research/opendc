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

package org.opendc.web.server.service;

import java.time.Instant;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import org.opendc.web.server.model.Portfolio;
import org.opendc.web.server.model.ProjectAuthorization;

/**
 * Service for managing {@link Portfolio}s.
 */
@ApplicationScoped
public final class PortfolioService {
    /**
     * List all {@link Portfolio}s that belong a certain project.
     */
    public List<org.opendc.web.proto.user.Portfolio> findByUser(String userId, long projectId) {
        // User must have access to project
        ProjectAuthorization auth = ProjectAuthorization.findByUser(userId, projectId);

        if (auth == null) {
            return List.of();
        }

        return Portfolio.findByProject(projectId).stream()
                .map((p) -> toUserDto(p, auth))
                .toList();
    }

    /**
     * Find a {@link Portfolio} with the specified <code>number</code> belonging to <code>projectId</code>.
     */
    public org.opendc.web.proto.user.Portfolio findByUser(String userId, long projectId, int number) {
        // User must have access to project
        ProjectAuthorization auth = ProjectAuthorization.findByUser(userId, projectId);

        if (auth == null) {
            return null;
        }

        Portfolio portfolio = Portfolio.findByProject(projectId, number);

        if (portfolio == null) {
            return null;
        }

        return toUserDto(portfolio, auth);
    }

    /**
     * Delete the portfolio with the specified <code>number</code> belonging to <code>projectId</code>.
     */
    public org.opendc.web.proto.user.Portfolio delete(String userId, long projectId, int number) {
        // User must have access to project
        ProjectAuthorization auth = ProjectAuthorization.findByUser(userId, projectId);

        if (auth == null) {
            return null;
        } else if (!auth.canEdit()) {
            throw new IllegalStateException("Not permitted to edit project");
        }

        Portfolio entity = Portfolio.findByProject(projectId, number);
        if (entity == null) {
            return null;
        }

        entity.delete();
        return toUserDto(entity, auth);
    }

    /**
     * Construct a new {@link Portfolio} with the specified name.
     */
    public org.opendc.web.proto.user.Portfolio create(
            String userId, long projectId, org.opendc.web.proto.user.Portfolio.Create request) {
        // User must have access to project
        ProjectAuthorization auth = ProjectAuthorization.findByUser(userId, projectId);

        if (auth == null) {
            return null;
        } else if (!auth.canEdit()) {
            throw new IllegalStateException("Not permitted to edit project");
        }

        var now = Instant.now();
        var project = auth.project;
        int number = project.allocatePortfolio(now);

        Portfolio portfolio = new Portfolio(project, number, request.getName(), request.getTargets());

        project.portfolios.add(portfolio);
        portfolio.persist();

        return toUserDto(portfolio, auth);
    }

    /**
     * Convert a {@link Portfolio} entity into a {@link org.opendc.web.proto.user.Portfolio} DTO.
     */
    public static org.opendc.web.proto.user.Portfolio toUserDto(Portfolio portfolio, ProjectAuthorization auth) {
        return new org.opendc.web.proto.user.Portfolio(
                portfolio.id,
                portfolio.number,
                ProjectService.toUserDto(auth),
                portfolio.name,
                portfolio.targets,
                portfolio.scenarios.stream().map(ScenarioService::toSummaryDto).toList());
    }

    /**
     * Convert a {@link Portfolio} entity into a {@link org.opendc.web.proto.user.Portfolio.Summary} DTO.
     */
    public static org.opendc.web.proto.user.Portfolio.Summary toSummaryDto(Portfolio portfolio) {
        return new org.opendc.web.proto.user.Portfolio.Summary(
                portfolio.id, portfolio.number, portfolio.name, portfolio.targets);
    }

    /**
     * Convert a {@link Portfolio} into a runner-facing DTO.
     */
    public static org.opendc.web.proto.runner.Portfolio toRunnerDto(Portfolio portfolio) {
        return new org.opendc.web.proto.runner.Portfolio(
                portfolio.id, portfolio.number, portfolio.name, portfolio.targets);
    }
}
