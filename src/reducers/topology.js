import {FETCH_LATEST_DATACENTER_SUCCEEDED, RESET_CURRENT_DATACENTER} from "../actions/topology";

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
