export const FETCH_EXPERIMENTS_OF_SIMULATION = "FETCH_EXPERIMENTS_OF_SIMULATION";
export const ADD_EXPERIMENT = "ADD_EXPERIMENT";
export const DELETE_EXPERIMENT = "DELETE_EXPERIMENT";

export function fetchExperimentsOfSimulation(simulationId) {
    return {
        type: FETCH_EXPERIMENTS_OF_SIMULATION,
        simulationId
    };
}

export function addExperiment(experiment) {
    return {
        type: ADD_EXPERIMENT,
        experiment
    };
}

export function deleteExperiment(id) {
    return {
        type: DELETE_EXPERIMENT,
        id
    };
}
