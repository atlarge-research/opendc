import { combineReducers } from 'redux'
import { auth } from './auth'
import { construction } from './construction-mode'
import { currentDatacenterId, currentSimulationId } from './current-ids'
import { interactionLevel } from './interaction-level'
import { map } from './map'
import { modals } from './modals'
import { objects } from './objects'
import { simulationList } from './simulation-list'
import { currentExperimentId, currentTick, isPlaying, lastSimulatedTick, loadMetric } from './simulation-mode'
import { states } from './states'

const rootReducer = combineReducers({
    objects,
    states,
    modals,
    simulationList,
    construction,
    map,
    currentSimulationId,
    currentDatacenterId,
    currentExperimentId,
    currentTick,
    lastSimulatedTick,
    loadMetric,
    isPlaying,
    interactionLevel,
    auth,
})

export default rootReducer
