export const OPEN_NEW_SIMULATION_MODAL = "OPEN_NEW_SIMULATION_MODAL";
export const CLOSE_NEW_SIMULATION_MODAL = "CLOSE_SIMULATION_MODAL";

export function openNewSimulationModal() {
    return {
        type: OPEN_NEW_SIMULATION_MODAL
    };
}

export function closeNewSimulationModal() {
    return {
        type: CLOSE_NEW_SIMULATION_MODAL
    };
}
