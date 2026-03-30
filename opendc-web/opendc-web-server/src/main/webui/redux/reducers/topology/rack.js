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
import { DELETE_MACHINE } from '../../actions/topology/machine'
import { DELETE_RACK, EDIT_RACK_NAME, ADD_MACHINE } from '../../actions/topology/rack'
import { ADD_RACK_TO_TILE } from '../../actions/topology/room'

function rack(state = {}, action, { machines }) {
    switch (action.type) {
        case STORE_TOPOLOGY:
            return action.entities.racks || {}
        case ADD_RACK_TO_TILE:
            return produce(state, (draft) => {
                const { rack } = action
                draft[rack.id] = rack
            })
        case EDIT_RACK_NAME:
            return produce(state, (draft) => {
                const { rackId, name } = action
                draft[rackId].name = name
            })
        case DELETE_RACK:
            return produce(state, (draft) => {
                const { rackId } = action
                delete draft[rackId]
            })
        case ADD_MACHINE:
            return produce(state, (draft) => {
                const { machine } = action
                draft[machine.rackId].machines.push(machine.id)
            })
        case DELETE_MACHINE:
            return produce(state, (draft) => {
                const { machineId } = action
                const machine = machines[machineId]
                const rack = draft[machine.rackId]
                const index = rack.machines.indexOf(machineId)
                rack.machines.splice(index, 1)
            })
        default:
            return state
    }
}

export default rack
