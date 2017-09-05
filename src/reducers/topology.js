import {FETCH_LATEST_DATACENTER_SUCCEEDED} from "../actions/topology";

export function currentDatacenterId(state = -1, action) {
    switch (action.type) {
        case FETCH_LATEST_DATACENTER_SUCCEEDED:
            return action.datacenterId;
        default:
            return state;
    }
}
