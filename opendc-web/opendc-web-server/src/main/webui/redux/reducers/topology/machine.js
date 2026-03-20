import produce from 'immer'
import { STORE_TOPOLOGY } from '../../actions/topology'
import { DELETE_MACHINE, ADD_UNIT, DELETE_UNIT } from '../../actions/topology/machine'
import { ADD_MACHINE, DELETE_RACK } from '../../actions/topology/rack'

function machine(state = {}, action, { racks }) {
    switch (action.type) {
        case STORE_TOPOLOGY:
            return action.entities.machines || {}
        case ADD_MACHINE:
            return produce(state, (draft) => {
                const { machine } = action
                draft[machine.id] = machine
            })
        case DELETE_MACHINE:
            return produce(state, (draft) => {
                const { machineId } = action
                delete draft[machineId]
            })
        case ADD_UNIT:
            return produce(state, (draft) => {
                const { machineId, unitType, unitId } = action
                draft[machineId][unitType].push(unitId)
            })
        case DELETE_UNIT:
            return produce(state, (draft) => {
                const { machineId, unitType, unitId } = action
                const units = draft[machineId][unitType]
                const index = units.indexOf(unitId)
                units.splice(index, 1)
            })
        case DELETE_RACK:
            return produce(state, (draft) => {
                const { rackId } = action
                const rack = racks[rackId]

                for (const id of rack.machines) {
                    const machine = draft[id]
                    machine.rackId = undefined
                }
            })
        default:
            return state
    }
}

export default machine
