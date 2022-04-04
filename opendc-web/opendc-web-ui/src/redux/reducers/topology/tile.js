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
import { ADD_TILE, DELETE_TILE } from '../../actions/topology/building'
import { DELETE_RACK } from '../../actions/topology/rack'
import { ADD_RACK_TO_TILE } from '../../actions/topology/room'

function tile(state = {}, action, { racks }) {
    switch (action.type) {
        case STORE_TOPOLOGY:
            return action.entities.tiles || {}
        case ADD_TILE:
            return produce(state, (draft) => {
                const { tile } = action
                draft[tile.id] = tile
            })
        case DELETE_TILE:
            return produce(state, (draft) => {
                const { tileId } = action
                delete draft[tileId]
            })
        case ADD_RACK_TO_TILE:
            return produce(state, (draft) => {
                const { rack } = action
                draft[rack.tileId].rack = rack.id
            })
        case DELETE_RACK:
            return produce(state, (draft) => {
                const { rackId } = action
                const rack = racks[rackId]
                draft[rack.tileId].rack = undefined
            })
        default:
            return state
    }
}

export default tile
