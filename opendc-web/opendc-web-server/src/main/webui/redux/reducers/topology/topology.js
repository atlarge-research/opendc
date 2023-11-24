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
import { ADD_ROOM, DELETE_ROOM } from '../../actions/topology/room'

function topology(state = undefined, action) {
    switch (action.type) {
        case STORE_TOPOLOGY:
            return action.topology
        case ADD_ROOM:
            return produce(state, (draft) => {
                const { room } = action
                draft.rooms.push(room.id)
            })
        case DELETE_ROOM:
            return produce(state, (draft) => {
                const { roomId } = action
                const index = draft.rooms.indexOf(roomId)
                draft.rooms.splice(index, 1)
            })
        default:
            return state
    }
}

export default topology
