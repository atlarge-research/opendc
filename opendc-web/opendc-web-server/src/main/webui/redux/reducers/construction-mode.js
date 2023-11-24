import { combineReducers } from 'redux'
import { GO_DOWN_ONE_INTERACTION_LEVEL } from '../actions/interaction-level'
import {
    CANCEL_NEW_ROOM_CONSTRUCTION_SUCCEEDED,
    FINISH_NEW_ROOM_CONSTRUCTION,
    FINISH_ROOM_EDIT,
    START_NEW_ROOM_CONSTRUCTION_SUCCEEDED,
    START_ROOM_EDIT,
} from '../actions/topology/building'
import { DELETE_ROOM, START_RACK_CONSTRUCTION, STOP_RACK_CONSTRUCTION } from '../actions/topology/room'

export function currentRoomInConstruction(state = '-1', action) {
    switch (action.type) {
        case START_NEW_ROOM_CONSTRUCTION_SUCCEEDED:
            return action.roomId
        case START_ROOM_EDIT:
            return action.roomId
        case CANCEL_NEW_ROOM_CONSTRUCTION_SUCCEEDED:
        case FINISH_NEW_ROOM_CONSTRUCTION:
        case FINISH_ROOM_EDIT:
        case DELETE_ROOM:
            return '-1'
        default:
            return state
    }
}

export function inRackConstructionMode(state = false, action) {
    switch (action.type) {
        case START_RACK_CONSTRUCTION:
            return true
        case STOP_RACK_CONSTRUCTION:
        case GO_DOWN_ONE_INTERACTION_LEVEL:
            return false
        default:
            return state
    }
}

export const construction = combineReducers({
    currentRoomInConstruction,
    inRackConstructionMode,
})
