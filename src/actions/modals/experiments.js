export const OPEN_NEW_EXPERIMENT_MODAL = "OPEN_NEW_EXPERIMENT_MODAL";
export const CLOSE_NEW_EXPERIMENT_MODAL = "CLOSE_EXPERIMENT_MODAL";

export function openNewExperimentModal() {
    return {
        type: OPEN_NEW_EXPERIMENT_MODAL
    };
}

export function closeNewExperimentModal() {
    return {
        type: CLOSE_NEW_EXPERIMENT_MODAL
    };
}
