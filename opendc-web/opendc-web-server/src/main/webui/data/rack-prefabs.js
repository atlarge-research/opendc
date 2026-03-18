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
import { addRackPrefab, deleteRackPrefab, fetchRackPrefabs } from '../api/rack-prefabs'

/**
 * Configure the query defaults for the rack prefab endpoints.
 */
export function configureRackPrefabClient(queryClient, auth) {
    queryClient.setQueryDefaults('rack-prefabs', {
        queryFn: ({ queryKey }) => fetchRackPrefabs(auth, queryKey[1]),
    })

    queryClient.setMutationDefaults('addRackPrefab', {
        mutationFn: ({ projectId, ...data }) => addRackPrefab(auth, projectId, data),
        onSuccess: (result) => {
            queryClient.setQueryData(['rack-prefabs', result.project.id], (old = []) => [...old, result])
        },
    })
    queryClient.setMutationDefaults('deleteRackPrefab', {
        mutationFn: ({ projectId, number }) => deleteRackPrefab(auth, projectId, number),
        onSuccess: (result) => {
            queryClient.setQueryData(['rack-prefabs', result.project.id], (old = []) =>
                old.filter((rackPrefab) => rackPrefab.id !== result.id)
            )
        },
    })
}

/**
 * Fetch all rack prefabs of the specified project.
 */
export function useRackPrefabs(projectId, options = {}) {
    return useQuery(['rack-prefabs', projectId], { enabled: !!projectId, ...options })
}

/**
 * Create a mutation for a new rack prefab.
 */
export function useNewRackPrefab() {
    return useMutation('addRackPrefab')
}

/**
 * Create a mutation for deleting a rack prefab.
 */
export function useDeleteRackPrefab(options = {}) {
    return useMutation('deleteRackPrefab', options)
}
