import {SIM_HIGH_COLOR, SIM_LOW_COLOR, SIM_MID_HIGH_COLOR, SIM_MID_LOW_COLOR} from "./colors";

export const LOAD_NAME_MAP = {
    LOAD: "computational load",
    TEMPERATURE: "temperature",
    MEMORY: "memory use",
};

export function convertLoadToSimulationColor(load) {
    if (load <= 0.25) {
        return SIM_LOW_COLOR;
    } else if (load <= 0.5) {
        return SIM_MID_LOW_COLOR;
    } else if (load <= 0.75) {
        return SIM_MID_HIGH_COLOR;
    } else {
        return SIM_HIGH_COLOR;
    }
}

export function getStateLoad(loadMetric, state) {
    switch (loadMetric) {
        case "LOAD":
            return state.loadFraction;
        case "TEMPERATURE":
            return state.temperatureC / 100.0;
        case "MEMORY":
            return state.inUseMemoryMb / 10000.0;
        default:
            return -1;
    }
}
