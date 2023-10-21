import {
    GO_DOWN_ONE_INTERACTION_LEVEL,
    GO_FROM_BUILDING_TO_ROOM,
    GO_FROM_RACK_TO_MACHINE,
    GO_FROM_ROOM_TO_RACK,
} from '../actions/interaction-level'
import { DELETE_MACHINE } from '../actions/topology/machine'
import { DELETE_RACK } from '../actions/topology/rack'
import { DELETE_ROOM } from '../actions/topology/room'

export function interactionLevel(state = { mode: 'BUILDING' }, action) {
    switch (action.type) {
        case GO_FROM_BUILDING_TO_ROOM:
            return {
                mode: 'ROOM',
                roomId: action.roomId,
            }
        case GO_FROM_ROOM_TO_RACK:
            return {
                mode: 'RACK',
                roomId: state.roomId,
                tileId: action.tileId,
            }
        case GO_FROM_RACK_TO_MACHINE:
            return {
                mode: 'MACHINE',
                roomId: state.roomId,
                tileId: state.tileId,
                position: action.position,
            }
        case GO_DOWN_ONE_INTERACTION_LEVEL:
            if (state.mode === 'ROOM') {
                return {
                    mode: 'BUILDING',
                }
            } else if (state.mode === 'RACK') {
                return {
                    mode: 'ROOM',
                    roomId: state.roomId,
                }
            } else if (state.mode === 'MACHINE') {
                return {
                    mode: 'RACK',
                    roomId: state.roomId,
                    tileId: state.tileId,
                }
            } else {
                return state
            }
        case DELETE_MACHINE:
            return {
                mode: 'RACK',
                roomId: state.roomId,
                tileId: state.tileId,
            }
        case DELETE_RACK:
            return {
                mode: 'ROOM',
                roomId: state.roomId,
            }
        case DELETE_ROOM:
            return {
                mode: 'BUILDING',
            }
        default:
            return state
    }
}
