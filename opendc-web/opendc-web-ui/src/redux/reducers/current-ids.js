import { SET_CURRENT_TOPOLOGY } from '../actions/topology/building'

export function currentTopologyId(state = '-1', action) {
    switch (action.type) {
        case SET_CURRENT_TOPOLOGY:
            return action.topologyId
        default:
            return state
    }
}
