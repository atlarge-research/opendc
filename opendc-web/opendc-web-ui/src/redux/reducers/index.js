import { combineReducers } from 'redux'
import { construction } from './construction-mode'
import { currentPortfolioId, currentProjectId, currentScenarioId, currentTopologyId } from './current-ids'
import { interactionLevel } from './interaction-level'
import { map } from './map'
import { objects } from './objects'
import { projects } from './projects'

const rootReducer = combineReducers({
    objects,
    projects,
    construction,
    map,
    currentProjectId,
    currentTopologyId,
    currentPortfolioId,
    currentScenarioId,
    interactionLevel,
})

export default rootReducer
