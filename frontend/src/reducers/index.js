import { combineReducers } from 'redux'
import { auth } from './auth'
import { construction } from './construction-mode'
import { currentPortfolioId, currentProjectId, currentScenarioId, currentTopologyId } from './current-ids'
import { interactionLevel } from './interaction-level'
import { map } from './map'
import { modals } from './modals'
import { objects } from './objects'
import { projectList } from './project-list'

const rootReducer = combineReducers({
    objects,
    modals,
    projectList,
    construction,
    map,
    currentProjectId,
    currentTopologyId,
    currentPortfolioId,
    currentScenarioId,
    interactionLevel,
    auth,
})

export default rootReducer
