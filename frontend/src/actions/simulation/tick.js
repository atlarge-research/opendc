export const GO_TO_TICK = 'GO_TO_TICK'
export const SET_LAST_SIMULATED_TICK = 'SET_LAST_SIMULATED_TICK'

export function incrementTick() {
    return (dispatch, getState) => {
        const { currentTick } = getState()
        dispatch(goToTick(currentTick + 1))
    }
}

export function goToTick(tick) {
    return (dispatch, getState) => {
        dispatch({
            type: GO_TO_TICK,
            tick,
        })
    }
}

export function setLastSimulatedTick(tick) {
    return {
        type: SET_LAST_SIMULATED_TICK,
        tick,
    }
}
