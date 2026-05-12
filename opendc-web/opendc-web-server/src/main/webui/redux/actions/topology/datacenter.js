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

import { v4 as uuid } from 'uuid'

export const ADD_DATACENTER = 'ADD_DATACENTER'
export const EDIT_DATACENTER_NAME = 'EDIT_DATACENTER_NAME'
export const DELETE_DATACENTER = 'DELETE_DATACENTER'
export const RESIZE_DATACENTER = 'RESIZE_DATACENTER'

export function addDatacenter(topologyId, datacenter) {
    return {
        type: ADD_DATACENTER,
        datacenter: {
            id: uuid(),
            topologyId,
            ...datacenter,
        },
    }
}

export function editDatacenterName(datacenterId, name) {
    return {
        type: EDIT_DATACENTER_NAME,
        datacenterId,
        name,
    }
}

export function deleteDatacenter(datacenterId) {
    return {
        type: DELETE_DATACENTER,
        datacenterId,
    }
}

export function resizeDatacenter(datacenterId, width, height) {
    return (dispatch, getState) => {
        const { topology } = getState()
        const { datacenters, rooms, tiles } = topology

        // Compute new DC x-positions after relayout (sorted by current x, target DC uses new width)
        const sorted = Object.values(datacenters)
            .sort((a, b) => (a.x ?? 0) - (b.x ?? 0))
            .map((dc) => (dc.id === datacenterId ? { ...dc, width } : dc))

        const datacenterPositions = {}
        let nextX = 0
        for (const dc of sorted) {
            datacenterPositions[dc.id] = { x: nextX, y: dc.y ?? 0 }
            nextX += dc.width + 1
        }

        // Compute tile moves for datacenters whose x changed
        const tileMoves = {}
        for (const [dcId, newPos] of Object.entries(datacenterPositions)) {
            const deltaX = newPos.x - (datacenters[dcId].x ?? 0)
            if (deltaX === 0) continue
            for (const roomId of datacenters[dcId].rooms ?? []) {
                for (const tileId of rooms[roomId]?.tiles ?? []) {
                    const tile = tiles[tileId]
                    if (tile) {
                        tileMoves[tileId] = { positionX: tile.positionX + deltaX, positionY: tile.positionY }
                    }
                }
            }
        }

        dispatch({ type: RESIZE_DATACENTER, datacenterId, width, height, datacenterPositions, tileMoves })
    }
}
