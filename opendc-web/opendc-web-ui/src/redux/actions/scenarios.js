export const ADD_SCENARIO = 'ADD_SCENARIO'
export const UPDATE_SCENARIO = 'UPDATE_SCENARIO'
export const DELETE_SCENARIO = 'DELETE_SCENARIO'
export const OPEN_SCENARIO_SUCCEEDED = 'OPEN_SCENARIO_SUCCEEDED'
export const SET_CURRENT_SCENARIO = 'SET_CURRENT_SCENARIO'

export function addScenario(scenario) {
    return {
        type: ADD_SCENARIO,
        scenario,
    }
}

export function updateScenario(scenario) {
    return {
        type: UPDATE_SCENARIO,
        scenario,
    }
}

export function deleteScenario(id) {
    return {
        type: DELETE_SCENARIO,
        id,
    }
}

export function openScenarioSucceeded(projectId, portfolioId, scenarioId) {
    return {
        type: OPEN_SCENARIO_SUCCEEDED,
        projectId,
        portfolioId,
        scenarioId,
    }
}

export function setCurrentScenario(portfolioId, scenarioId) {
    return {
        type: SET_CURRENT_SCENARIO,
        portfolioId,
        scenarioId,
    }
}
