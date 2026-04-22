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

import produce from 'immer'
import { STORE_TOPOLOGY } from '../../actions/topology'
import { ADD_DATACENTER, DELETE_DATACENTER, EDIT_DATACENTER_NAME, RESIZE_DATACENTER } from '../../actions/topology/datacenter'
import { ADD_ROOM, DELETE_ROOM } from '../../actions/topology/room'

function datacenter(state = {}, action, { rooms } = {}) {
    switch (action.type) {
        case STORE_TOPOLOGY:
            return action.entities.datacenters || {}
        case ADD_DATACENTER:
            return produce(state, (draft) => {
                const { datacenter } = action
                draft[datacenter.id] = datacenter
            })
        case DELETE_DATACENTER:
            return produce(state, (draft) => {
                const { datacenterId } = action
                delete draft[datacenterId]
            })
        case EDIT_DATACENTER_NAME:
            return produce(state, (draft) => {
                const { datacenterId, name } = action
                draft[datacenterId].name = name
            })
        case RESIZE_DATACENTER:
            return produce(state, (draft) => {
                const { datacenterId, width, height, datacenterPositions } = action
                draft[datacenterId].width = width
                draft[datacenterId].height = height
                for (const [id, pos] of Object.entries(datacenterPositions ?? {})) {
                    draft[id].x = pos.x
                    draft[id].y = pos.y
                }
            })
        case ADD_ROOM:
            return produce(state, (draft) => {
                const { room } = action
                draft[room.datacenterId].rooms.push(room.id)
            })
        case DELETE_ROOM:
            return produce(state, (draft) => {
                const { roomId } = action
                const room = rooms[roomId]
                if (!room) return
                const dc = draft[room.datacenterId]
                if (!dc) return
                const index = dc.rooms.indexOf(roomId)
                if (index !== -1) dc.rooms.splice(index, 1)
            })
        default:
            return state
    }
}

export default datacenter
