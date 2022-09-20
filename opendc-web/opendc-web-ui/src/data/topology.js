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
import { addTopology, deleteTopology, fetchTopologies, fetchTopology, updateTopology } from '../api/topologies'

/**
 * Configure the query defaults for the topology endpoints.
 */
export function configureTopologyClient(queryClient, auth) {
    queryClient.setQueryDefaults('topologies', {
        queryFn: ({ queryKey }) =>
            queryKey.length === 2 ? fetchTopologies(auth, queryKey[1]) : fetchTopology(auth, queryKey[1], queryKey[2]),
    })

    queryClient.setMutationDefaults('addTopology', {
        mutationFn: ({ projectId, ...data }) => addTopology(auth, projectId, data),
        onSuccess: (result) => {
            queryClient.setQueryData(['topologies', result.project.id], (old = []) => [...old, result])
            queryClient.setQueryData(['topologies', result.project.id, result.number], result)
        },
    })
    queryClient.setMutationDefaults('updateTopology', {
        mutationFn: (data) => updateTopology(auth, data),
        onSuccess: (result) => {
            queryClient.setQueryData(['topologies', result.project.id], (old = []) =>
                old.map((topology) => (topology.id === result.id ? result : topology))
            )
            queryClient.setQueryData(['topologies', result.project.id, result.number], result)
        },
    })
    queryClient.setMutationDefaults('deleteTopology', {
        mutationFn: ({ projectId, number }) => deleteTopology(auth, projectId, number),
        onSuccess: (result) => {
            queryClient.setQueryData(['topologies', result.project.id], (old = []) =>
                old.filter((topology) => topology.id !== result.id)
            )
            queryClient.removeQueries(['topologies', result.project.id, result.number])
        },
    })
}

/**
 * Fetch the topology with the specified identifier for the specified project.
 */
export function useTopology(projectId, topologyId, options = {}) {
    return useQuery(['topologies', projectId, topologyId], { enabled: !!(projectId && topologyId), ...options })
}

/**
 * Fetch all topologies of the specified project.
 */
export function useTopologies(projectId, options = {}) {
    return useQuery(['topologies', projectId], { enabled: !!projectId, ...options })
}

/**
 * Create a mutation for a new topology.
 */
export function useNewTopology() {
    return useMutation('addTopology')
}

/**
 * Create a mutation for deleting a topology.
 */
export function useDeleteTopology() {
    return useMutation('deleteTopology')
}
