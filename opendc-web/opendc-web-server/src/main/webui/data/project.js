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
import { addExperiment, deleteExperiment, fetchExperiment, fetchExperiments } from '../api/experiments'
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

    queryClient.setQueryDefaults('experiments', {
        queryFn: ({ queryKey }) =>
            queryKey.length === 2 ? fetchExperiments(auth, queryKey[1]) : fetchExperiment(auth, queryKey[1], queryKey[2]),
    })
    queryClient.setMutationDefaults('addExperiment', {
        mutationFn: ({ projectId, ...data }) => addExperiment(auth, projectId, data),
        onSuccess: async (result) => {
            queryClient.setQueryData(['experiments', result.project.id], (old = []) => [...old, result])
            queryClient.setQueryData(['experiments', result.project.id, result.number], result)
        },
    })
    queryClient.setMutationDefaults('deleteExperiment', {
        mutationFn: ({ projectId, number }) => deleteExperiment(auth, projectId, number),
        onSuccess: async (result) => {
            queryClient.setQueryData(['experiments', result.project.id], (old = []) =>
                old.filter((experiment) => experiment.id !== result.id)
            )
            queryClient.removeQueries(['experiments', result.project.id, result.number])
        },
    })

    queryClient.setQueryDefaults('scenarios', {
        queryFn: ({ queryKey }) => fetchScenario(auth, queryKey[1], queryKey[2]),
    })
    queryClient.setMutationDefaults('addScenario', {
        mutationFn: ({ projectId, experimentNumber, data }) => addScenario(auth, projectId, experimentNumber, data),
        onSuccess: async (result) => {
            // Register updated scenario in cache
            queryClient.setQueryData(['scenarios', result.project.id, result.id], result)
            queryClient.setQueryData(['experiments', result.project.id, result.experiment.number], (old) => ({
                ...old,
                scenarios: [...old.scenarios, result],
            }))
        },
    })
    queryClient.setMutationDefaults('deleteScenario', {
        mutationFn: ({ projectId, number }) => deleteScenario(auth, projectId, number),
        onSuccess: async (result) => {
            queryClient.removeQueries(['scenarios', result.project.id, result.id])
            queryClient.setQueryData(['experiments', result.project.id, result.experiment.number], (old) => ({
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
 * Return the experiment with the specified identifier.
 */
export function useExperiment(projectId, experimentId, options = {}) {
    return useQuery(['experiments', projectId, experimentId], { enabled: !!(projectId && experimentId), ...options })
}

/**
 * Return the experiments of the specified project.
 */
export function useExperiments(projectId, options = {}) {
    return useQuery(['experiments', projectId], { enabled: !!projectId, ...options })
}

/**
 * Create a mutation for a new experiment.
 */
export function useNewExperiment() {
    return useMutation('addExperiment')
}

/**
 * Create a mutation for deleting an experiment.
 */
export function useDeleteExperiment() {
    return useMutation('deleteExperiment')
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

/**
 * Return the job report for the specified job.
 */
export function useJobReport(jobId, options = {}) {
    return useQuery(
        ['job-report', jobId],
        async () => {
            const response = await fetch(`/api/jobs/${jobId}/report`)
            if (!response.ok) {
                throw new Error('Failed to fetch job report')
            }
            return response.json()
        },
        { enabled: !!jobId, ...options }
    )
}
