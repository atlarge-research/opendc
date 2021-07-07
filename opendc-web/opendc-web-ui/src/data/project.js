/*
 * Copyright (c) 2021 AtLarge Research
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

import { useSelector } from 'react-redux'

/**
 * Return the available projects.
 */
export function useProjects() {
    return useSelector((state) => state.projects)
}

/**
 * Return the project with the specified identifier.
 */
export function useProject(projectId) {
    return useSelector((state) => state.projects[projectId])
}

/**
 * Return the current active project.
 */
export function useActiveProject() {
    return useSelector((state) =>
        state.currentProjectId !== '-1' ? state.objects.project[state.currentProjectId] : undefined
    )
}

/**
 * Return the active portfolio.
 */
export function useActivePortfolio() {
    return useSelector((state) => state.objects.portfolio[state.currentPortfolioId])
}

/**
 * Return the active scenario.
 */
export function useActiveScenario() {
    return useSelector((state) => state.objects.scenario[state.currentScenarioId])
}

/**
 * Return the portfolios for the specified project id.
 */
export function usePortfolios(projectId) {
    return useSelector((state) => {
        let portfolios = state.objects.project[projectId]
            ? state.objects.project[projectId].portfolioIds.map((t) => state.objects.portfolio[t])
            : []
        if (portfolios.filter((t) => !t).length > 0) {
            portfolios = []
        }

        return portfolios
    })
}

/**
 * Return the scenarios for the specified portfolio id.
 */
export function useScenarios(portfolioId) {
    return useSelector((state) => {
        let scenarios = state.objects.portfolio[portfolioId]
            ? state.objects.portfolio[portfolioId].scenarioIds.map((t) => state.objects.scenario[t])
            : []
        if (scenarios.filter((t) => !t).length > 0) {
            scenarios = []
        }

        return scenarios
    })
}
