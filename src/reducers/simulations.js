import {OPEN_SIMULATION_SUCCEEDED} from "../actions/simulations";

export function currentSimulationId(state = -1, action) {
    switch (action.type) {
        case OPEN_SIMULATION_SUCCEEDED:
            return action.id;
        default:
            return state;
    }
}
