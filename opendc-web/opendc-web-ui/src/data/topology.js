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
import { useQuery } from 'react-query'
import { addTopology, deleteTopology, fetchTopologiesOfProject, fetchTopology, updateTopology } from '../api/topologies'

/**
 * Configure the query defaults for the topology endpoints.
 */
export function configureTopologyClient(queryClient, auth) {
    queryClient.setQueryDefaults('topologies', { queryFn: ({ queryKey }) => fetchTopology(auth, queryKey[1]) })
    queryClient.setQueryDefaults('project-topologies', {
        queryFn: ({ queryKey }) => fetchTopologiesOfProject(auth, queryKey[1]),
    })

    queryClient.setMutationDefaults('addTopology', {
        mutationFn: (data) => addTopology(auth, data),
        onSuccess: async (result) => {
            queryClient.setQueryData(['projects', result.projectId], (old) => ({
                ...old,
                topologyIds: [...old.topologyIds, result._id],
            }))
            queryClient.setQueryData(['project-topologies', result.projectId], (old = []) => [...old, result])
            queryClient.setQueryData(['topologies', result._id], result)
        },
    })
    queryClient.setMutationDefaults('updateTopology', {
        mutationFn: (data) => updateTopology(auth, data),
        onSuccess: async (result) => queryClient.setQueryData(['topologies', result._id], result),
    })
    queryClient.setMutationDefaults('deleteTopology', {
        mutationFn: (id) => deleteTopology(auth, id),
        onSuccess: async (result) => {
            queryClient.setQueryData(['projects', result.projectId], (old) => ({
                ...old,
                topologyIds: old.topologyIds.filter((id) => id !== result._id),
            }))
            queryClient.setQueryData(['project-topologies', result.projectId], (old = []) =>
                old.filter((topology) => topology._id !== result._id)
            )
            queryClient.removeQueries(['topologies', result._id])
        },
    })
}

/**
 * Return the current active topology.
 */
export function useActiveTopology() {
    return useSelector((state) => state.currentTopologyId !== '-1' && state.objects.topology[state.currentTopologyId])
}

/**
 * Return the current active topology.
 */
export function useTopology(topologyId, options = {}) {
    return useQuery(['topologies', topologyId], { enabled: !!topologyId, ...options })
}

/**
 * Return the topologies of the specified project.
 */
export function useProjectTopologies(projectId, options = {}) {
    return useQuery(['project-topologies', projectId], { enabled: !!projectId, ...options })
}
