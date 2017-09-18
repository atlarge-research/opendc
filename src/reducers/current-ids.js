import {OPEN_EXPERIMENT_SUCCEEDED} from "../actions/experiments";
import {OPEN_SIMULATION_SUCCEEDED} from "../actions/simulations";
import {RESET_CURRENT_DATACENTER, SET_CURRENT_DATACENTER} from "../actions/topology/building";

export function currentDatacenterId(state = -1, action) {
    switch (action.type) {
        case SET_CURRENT_DATACENTER:
            return action.datacenterId;
        case RESET_CURRENT_DATACENTER:
            return -1;
        default:
            return state;
    }
}

export function currentSimulationId(state = -1, action) {
    switch (action.type) {
        case OPEN_SIMULATION_SUCCEEDED:
            return action.id;
        case OPEN_EXPERIMENT_SUCCEEDED:
            return action.simulationId;
        default:
            return state;
    }
}

