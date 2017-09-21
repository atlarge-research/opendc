import {OPEN_EXPERIMENT_SUCCEEDED} from "../actions/experiments";
import {CHANGE_LOAD_METRIC} from "../actions/simulation/load-metric";
import {SET_PLAYING} from "../actions/simulation/playback";
import {GO_TO_TICK, SET_LAST_SIMULATED_TICK} from "../actions/simulation/tick";
import {OPEN_SIMULATION_SUCCEEDED} from "../actions/simulations";

export function currentExperimentId(state = -1, action) {
    switch (action.type) {
        case OPEN_EXPERIMENT_SUCCEEDED:
            return action.experimentId;
        case OPEN_SIMULATION_SUCCEEDED:
            return -1;
        default:
            return state;
    }
}

export function currentTick(state = 0, action) {
    switch (action.type) {
        case GO_TO_TICK:
            return action.tick;
        case OPEN_EXPERIMENT_SUCCEEDED:
            return 0;
        default:
            return state;
    }
}

export function loadMetric(state = "LOAD", action) {
    switch (action.type) {
        case CHANGE_LOAD_METRIC:
            return action.metric;
        default:
            return state;
    }
}

export function isPlaying(state = false, action) {
    switch (action.type) {
        case SET_PLAYING:
            return action.playing;
        case OPEN_EXPERIMENT_SUCCEEDED:
            return false;
        default:
            return state;
    }
}

export function lastSimulatedTick(state = -1, action) {
    switch (action.type) {
        case SET_LAST_SIMULATED_TICK:
            return action.tick;
        case OPEN_EXPERIMENT_SUCCEEDED:
            return -1;
        default:
            return state;
    }
}

