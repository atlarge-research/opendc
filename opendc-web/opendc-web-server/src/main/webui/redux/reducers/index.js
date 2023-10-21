import { combineReducers } from 'redux'
import { construction } from './construction-mode'
import { interactionLevel } from './interaction-level'
import topology from './topology'

const rootReducer = combineReducers({
    topology,
    construction,
    interactionLevel,
})

export default rootReducer
