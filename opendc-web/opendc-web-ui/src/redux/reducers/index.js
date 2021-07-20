import { combineReducers } from 'redux'
import { construction } from './construction-mode'
import { currentTopologyId } from './current-ids'
import { interactionLevel } from './interaction-level'
import { objects } from './objects'

const rootReducer = combineReducers({
    objects,
    construction,
    currentTopologyId,
    interactionLevel,
})

export default rootReducer
