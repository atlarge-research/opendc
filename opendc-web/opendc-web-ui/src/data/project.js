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

import { useQueries, useQuery } from 'react-query'
import { fetchProject, fetchProjects } from '../api/projects'
import { useAuth } from '../auth'
import { useRouter } from 'next/router'
import { fetchPortfolio } from '../api/portfolios'
import { fetchScenario } from '../api/scenarios'

/**
 * Return the available projects.
 */
export function useProjects() {
    const auth = useAuth()
    return useQuery('projects', () => fetchProjects(auth))
}

/**
 * Return the project with the specified identifier.
 */
export function useProject(projectId) {
    const auth = useAuth()
    return useQuery(['projects', projectId], () => fetchProject(auth, projectId), { enabled: !!projectId })
}

/**
 * Return the portfolio with the specified identifier.
 */
export function usePortfolio(portfolioId) {
    const auth = useAuth()
    return useQuery(['portfolios', portfolioId], () => fetchPortfolio(auth, portfolioId), { enabled: !!portfolioId })
}

/**
 * Return the portfolios for the specified project id.
 */
export function usePortfolios(portfolioIds) {
    const auth = useAuth()
    return useQueries(
        portfolioIds.map((portfolioId) => ({
            queryKey: ['portfolios', portfolioId],
            queryFn: () => fetchPortfolio(auth, portfolioId),
        }))
    )
}

/**
 * Return the scenarios with the specified identifiers.
 */
export function useScenarios(scenarioIds) {
    const auth = useAuth()
    return useQueries(
        scenarioIds.map((scenarioId) => ({
            queryKey: ['scenario', scenarioId],
            queryFn: () => fetchScenario(auth, scenarioId),
        }))
    )
}

/**
 * Return the current active project identifier.
 */
export function useActiveProjectId() {
    const router = useRouter()
    const { project } = router.query
    return project
}
