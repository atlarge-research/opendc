import { combineReducers } from 'redux'
import { construction } from './construction-mode'
import { currentTopologyId } from './current-ids'
import { interactionLevel } from './interaction-level'
import { map } from './map'
import { objects } from './objects'

const rootReducer = combineReducers({
    objects,
    construction,
    map,
    currentTopologyId,
    interactionLevel,
})

export default rootReducer
