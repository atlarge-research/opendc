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
import { addProject, deleteProject, fetchProject, fetchProjects } from '../api/projects'
import { useRouter } from 'next/router'
import { addPortfolio, deletePortfolio, fetchPortfolio, fetchPortfoliosOfProject } from '../api/portfolios'
import { addScenario, deleteScenario, fetchScenario, fetchScenariosOfPortfolio } from '../api/scenarios'

/**
 * Configure the query defaults for the project endpoints.
 */
export function configureProjectClient(queryClient, auth) {
    queryClient.setQueryDefaults('projects', {
        queryFn: ({ queryKey }) => (queryKey.length === 1 ? fetchProjects(auth) : fetchProject(auth, queryKey[1])),
    })

    queryClient.setMutationDefaults('addProject', {
        mutationFn: (data) => addProject(auth, data),
        onSuccess: async (result) => {
            queryClient.setQueryData('projects', (old = []) => [...old, result])
        },
    })
    queryClient.setMutationDefaults('deleteProject', {
        mutationFn: (id) => deleteProject(auth, id),
        onSuccess: async (result) => {
            queryClient.setQueryData('projects', (old = []) => old.filter((project) => project._id !== result._id))
            queryClient.removeQueries(['projects', result._id])
        },
    })

    queryClient.setQueryDefaults('portfolios', {
        queryFn: ({ queryKey }) => fetchPortfolio(auth, queryKey[1]),
    })
    queryClient.setQueryDefaults('project-portfolios', {
        queryFn: ({ queryKey }) => fetchPortfoliosOfProject(auth, queryKey[1]),
    })
    queryClient.setMutationDefaults('addPortfolio', {
        mutationFn: (data) => addPortfolio(auth, data),
        onSuccess: async (result) => {
            queryClient.setQueryData(['projects', result.projectId], (old) => ({
                ...old,
                portfolioIds: [...old.portfolioIds, result._id],
            }))
            queryClient.setQueryData(['portfolios', result._id], result)
        },
    })
    queryClient.setMutationDefaults('deletePortfolio', {
        mutationFn: (id) => deletePortfolio(auth, id),
        onSuccess: async (result) => {
            queryClient.setQueryData(['projects', result.projectId], (old) => ({
                ...old,
                portfolioIds: old.portfolioIds.filter((id) => id !== result._id),
            }))
            queryClient.removeQueries(['portfolios', result._id])
        },
    })

    queryClient.setQueryDefaults('scenarios', {
        queryFn: ({ queryKey }) => fetchScenario(auth, queryKey[1]),
    })
    queryClient.setQueryDefaults('portfolio-scenarios', {
        queryFn: ({ queryKey }) => fetchScenariosOfPortfolio(auth, queryKey[1]),
    })
    queryClient.setMutationDefaults('addScenario', {
        mutationFn: (data) => addScenario(auth, data),
        onSuccess: async (result) => {
            // Register updated scenario in cache
            queryClient.setQueryData(['scenarios', result._id], result)

            // Add scenario id to portfolio
            queryClient.setQueryData(['portfolios', result.portfolioId], (old) => ({
                ...old,
                scenarioIds: [...old.scenarioIds, result._id],
            }))
        },
    })
    queryClient.setMutationDefaults('deleteScenario', {
        mutationFn: (id) => deleteScenario(auth, id),
        onSuccess: async (result) => {
            queryClient.setQueryData(['portfolios', result.portfolioId], (old) => ({
                ...old,
                scenarioIds: old.scenarioIds.filter((id) => id !== result._id),
            }))
            queryClient.removeQueries(['scenarios', result._id])
        },
    })
}

/**
 * Return the available projects.
 */
export function useProjects() {
    return useQuery('projects')
}

/**
 * Return the project with the specified identifier.
 */
export function useProject(projectId) {
    return useQuery(['projects', projectId], { enabled: !!projectId })
}

/**
 * Return the portfolio with the specified identifier.
 */
export function usePortfolio(portfolioId) {
    return useQuery(['portfolios', portfolioId], { enabled: !!portfolioId })
}

/**
 * Return the portfolios of the specified project.
 */
export function useProjectPortfolios(projectId) {
    return useQuery(['project-portfolios', projectId], { enabled: !!projectId })
}

/**
 * Return the scenarios with the specified identifiers.
 */
export function useScenarios(scenarioIds) {
    return useQueries(
        scenarioIds.map((scenarioId) => ({
            queryKey: ['scenarios', scenarioId],
        }))
    )
}

/**
 * Return the scenarios of the specified portfolio.
 */
export function usePortfolioScenarios(portfolioId) {
    return useQuery(['portfolio-scenarios', portfolioId], { enabled: !!portfolioId })
}

/**
 * Return the current active project identifier.
 */
export function useActiveProjectId() {
    const router = useRouter()
    const { project } = router.query
    return project
}
