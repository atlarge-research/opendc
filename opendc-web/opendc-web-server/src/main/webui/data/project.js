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

import { useQuery, useMutation } from 'react-query'
import { addProject, deleteProject, fetchProject, fetchProjects } from '../api/projects'
import { addPortfolio, deletePortfolio, fetchPortfolio, fetchPortfolios } from '../api/portfolios'
import { addScenario, deleteScenario, fetchScenario } from '../api/scenarios'

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
            queryClient.setQueryData(['projects', result.id], result)
        },
    })
    queryClient.setMutationDefaults('deleteProject', {
        mutationFn: (id) => deleteProject(auth, id),
        onSuccess: async (result) => {
            queryClient.setQueryData('projects', (old = []) => old.filter((project) => project.id !== result.id))
            queryClient.removeQueries(['projects', result.id])
        },
    })

    queryClient.setQueryDefaults('portfolios', {
        queryFn: ({ queryKey }) =>
            queryKey.length === 2 ? fetchPortfolios(auth, queryKey[1]) : fetchPortfolio(auth, queryKey[1], queryKey[2]),
    })
    queryClient.setMutationDefaults('addPortfolio', {
        mutationFn: ({ projectId, ...data }) => addPortfolio(auth, projectId, data),
        onSuccess: async (result) => {
            queryClient.setQueryData(['portfolios', result.project.id], (old = []) => [...old, result])
            queryClient.setQueryData(['portfolios', result.project.id, result.number], result)
        },
    })
    queryClient.setMutationDefaults('deletePortfolio', {
        mutationFn: ({ projectId, number }) => deletePortfolio(auth, projectId, number),
        onSuccess: async (result) => {
            queryClient.setQueryData(['portfolios', result.project.id], (old = []) =>
                old.filter((portfolio) => portfolio.id !== result.id)
            )
            queryClient.removeQueries(['portfolios', result.project.id, result.number])
        },
    })

    queryClient.setQueryDefaults('scenarios', {
        queryFn: ({ queryKey }) => fetchScenario(auth, queryKey[1], queryKey[2]),
    })
    queryClient.setMutationDefaults('addScenario', {
        mutationFn: ({ projectId, portfolioNumber, data }) => addScenario(auth, projectId, portfolioNumber, data),
        onSuccess: async (result) => {
            // Register updated scenario in cache
            queryClient.setQueryData(['scenarios', result.project.id, result.id], result)
            queryClient.setQueryData(['portfolios', result.project.id, result.portfolio.number], (old) => ({
                ...old,
                scenarios: [...old.scenarios, result],
            }))
        },
    })
    queryClient.setMutationDefaults('deleteScenario', {
        mutationFn: ({ projectId, number }) => deleteScenario(auth, projectId, number),
        onSuccess: async (result) => {
            queryClient.removeQueries(['scenarios', result.project.id, result.id])
            queryClient.setQueryData(['portfolios', result.project.id, result.portfolio.number], (old) => ({
                ...old,
                scenarios: old?.scenarios?.filter((scenario) => scenario.id !== result.id),
            }))
        },
    })
}

/**
 * Return the available projects.
 */
export function useProjects(options = {}) {
    return useQuery('projects', options)
}

/**
 * Return the project with the specified identifier.
 */
export function useProject(projectId, options = {}) {
    return useQuery(['projects', projectId], { enabled: !!projectId, ...options })
}

/**
 * Create a mutation for a new project.
 */
export function useNewProject() {
    return useMutation('addProject')
}

/**
 * Create a mutation for deleting a project.
 */
export function useDeleteProject() {
    return useMutation('deleteProject')
}

/**
 * Return the portfolio with the specified identifier.
 */
export function usePortfolio(projectId, portfolioId, options = {}) {
    return useQuery(['portfolios', projectId, portfolioId], { enabled: !!(projectId && portfolioId), ...options })
}

/**
 * Return the portfolios of the specified project.
 */
export function usePortfolios(projectId, options = {}) {
    return useQuery(['portfolios', projectId], { enabled: !!projectId, ...options })
}

/**
 * Create a mutation for a new portfolio.
 */
export function useNewPortfolio() {
    return useMutation('addPortfolio')
}

/**
 * Create a mutation for deleting a portfolio.
 */
export function useDeletePortfolio() {
    return useMutation('deletePortfolio')
}

/**
 * Create a mutation for a new scenario.
 */
export function useNewScenario() {
    return useMutation('addScenario')
}

/**
 * Create a mutation for deleting a scenario.
 */
export function useDeleteScenario() {
    return useMutation('deleteScenario')
}
