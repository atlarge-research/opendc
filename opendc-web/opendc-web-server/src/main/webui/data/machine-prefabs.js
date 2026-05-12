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
import { addMachinePrefab, deleteMachinePrefab, fetchMachinePrefabs } from '../api/machine-prefabs'

/**
 * Configure the query defaults for the machine prefab endpoints.
 */
export function configureMachinePrefabClient(queryClient, auth) {
    queryClient.setQueryDefaults('machine-prefabs', {
        queryFn: ({ queryKey }) => fetchMachinePrefabs(auth, queryKey[1]),
    })

    queryClient.setMutationDefaults('addMachinePrefab', {
        mutationFn: ({ projectId, ...data }) => addMachinePrefab(auth, projectId, data),
        onSuccess: (result) => {
            queryClient.setQueryData(['machine-prefabs', result.project.id], (old = []) => [...old, result])
        },
    })
    queryClient.setMutationDefaults('deleteMachinePrefab', {
        mutationFn: ({ projectId, number }) => deleteMachinePrefab(auth, projectId, number),
        onSuccess: (result) => {
            queryClient.setQueryData(['machine-prefabs', result.project.id], (old = []) =>
                old.filter((machinePrefab) => machinePrefab.id !== result.id)
            )
        },
    })
}

/**
 * Fetch all machine prefabs of the specified project.
 */
export function useMachinePrefabs(projectId, options = {}) {
    return useQuery(['machine-prefabs', projectId], { enabled: !!projectId, ...options })
}

/**
 * Create a mutation for a new machine prefab.
 */
export function useNewMachinePrefab() {
    return useMutation('addMachinePrefab')
}

/**
 * Create a mutation for deleting a machine prefab.
 */
export function useDeleteMachinePrefab(options = {}) {
    return useMutation('deleteMachinePrefab', options)
}
