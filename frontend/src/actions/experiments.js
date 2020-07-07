export const FETCH_EXPERIMENTS_OF_PROJECT = 'FETCH_EXPERIMENTS_OF_PROJECT'
export const ADD_EXPERIMENT = 'ADD_EXPERIMENT'
export const DELETE_EXPERIMENT = 'DELETE_EXPERIMENT'
export const OPEN_EXPERIMENT_SUCCEEDED = 'OPEN_EXPERIMENT_SUCCEEDED'

export function fetchExperimentsOfProject(projectId) {
    return {
        type: FETCH_EXPERIMENTS_OF_PROJECT,
        projectId,
    }
}

export function addExperiment(experiment) {
    return {
        type: ADD_EXPERIMENT,
        experiment,
    }
}

export function deleteExperiment(id) {
    return {
        type: DELETE_EXPERIMENT,
        id,
    }
}

export function openExperimentSucceeded(projectId, experimentId) {
    return {
        type: OPEN_EXPERIMENT_SUCCEEDED,
        projectId,
        experimentId,
    }
}
