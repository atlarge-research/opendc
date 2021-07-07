import { combineReducers } from 'redux'
import { construction } from './construction-mode'
import { currentTopologyId } from './current-ids'
import { interactionLevel } from './interaction-level'
import { map } from './map'
import { objects } from './objects'
import { projects } from './projects'

const rootReducer = combineReducers({
    objects,
    projects,
    construction,
    map,
    currentTopologyId,
    interactionLevel,
})

export default rootReducer
