import { combineReducers } from 'redux'
import { auth } from './auth'
import { construction } from './construction-mode'
import { currentProjectId, currentTopologyId } from './current-ids'
import { interactionLevel } from './interaction-level'
import { map } from './map'
import { modals } from './modals'
import { objects } from './objects'
import { projectList } from './project-list'

const rootReducer = combineReducers({
    objects,
    modals,
    projectList: projectList,
    construction,
    map,
    currentProjectId,
    currentTopologyId,
    interactionLevel,
    auth,
})

export default rootReducer
