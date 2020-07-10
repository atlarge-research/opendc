export const OPEN_NEW_SCENARIO_MODAL = 'OPEN_NEW_SCENARIO_MODAL'
export const CLOSE_NEW_SCENARIO_MODAL = 'CLOSE_SCENARIO_MODAL'

export function openNewScenarioModal() {
    return {
        type: OPEN_NEW_SCENARIO_MODAL,
    }
}

export function closeNewScenarioModal() {
    return {
        type: CLOSE_NEW_SCENARIO_MODAL,
    }
}
