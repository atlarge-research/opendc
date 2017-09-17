import {OPEN_SIMULATION_SUCCEEDED} from "../actions/simulations";
import {FETCH_LATEST_DATACENTER_SUCCEEDED, RESET_CURRENT_DATACENTER} from "../actions/topology/building";

export function currentDatacenterId(state = -1, action) {
    switch (action.type) {
        case FETCH_LATEST_DATACENTER_SUCCEEDED:
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
        default:
            return state;
    }
}

