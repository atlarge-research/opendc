import { OPEN_EXPERIMENT_SUCCEEDED } from '../actions/experiments'
import { OPEN_PROJECT_SUCCEEDED } from '../actions/projects'
import { RESET_CURRENT_TOPOLOGY, SET_CURRENT_TOPOLOGY } from '../actions/topology/building'

export function currentTopologyId(state = '-1', action) {
    switch (action.type) {
        case SET_CURRENT_TOPOLOGY:
            return action.topologyId
        case RESET_CURRENT_TOPOLOGY:
            return '-1'
        default:
            return state
    }
}

export function currentProjectId(state = '-1', action) {
    switch (action.type) {
        case OPEN_PROJECT_SUCCEEDED:
            return action.id
        case OPEN_EXPERIMENT_SUCCEEDED:
            return action.projectId
        default:
            return state
    }
}
