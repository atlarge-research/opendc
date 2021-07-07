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
import { useActiveProjectId, useProject } from './project'

/**
 * Return the current active topology.
 */
export function useActiveTopology() {
    return useSelector((state) => state.currentTopologyId !== '-1' && state.objects.topology[state.currentTopologyId])
}

/**
 * Return the topologies for the active project.
 */
export function useProjectTopologies() {
    const projectId = useActiveProjectId()
    const { data: project } = useProject(projectId)
    return useSelector(({ objects }) => {
        if (!project) {
            return []
        }

        const topologies = project.topologyIds.map((t) => objects.topology[t])

        if (topologies.filter((t) => !t).length > 0) {
            return []
        }

        return topologies
    })
}
