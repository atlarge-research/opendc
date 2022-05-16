import { v4 as uuid } from 'uuid'
import {
    DEFAULT_RACK_SLOT_CAPACITY,
    DEFAULT_RACK_POWER_CAPACITY,
} from '../../../components/topologies/map/MapConstants'
import { findTileWithPosition } from '../../../util/tile-calculations'

export const ADD_ROOM = 'ADD_ROOM'
export const EDIT_ROOM_NAME = 'EDIT_ROOM_NAME'
export const DELETE_ROOM = 'DELETE_ROOM'
export const START_RACK_CONSTRUCTION = 'START_RACK_CONSTRUCTION'
export const STOP_RACK_CONSTRUCTION = 'STOP_RACK_CONSTRUCTION'
export const ADD_RACK_TO_TILE = 'ADD_RACK_TO_TILE'

export function addRoom(topologyId, room) {
    return {
        type: ADD_ROOM,
        room: {
            id: uuid(),
            topologyId,
            ...room,
        },
    }
}

export function editRoomName(roomId, name) {
    return {
        type: EDIT_ROOM_NAME,
        name,
        roomId,
    }
}

export function startRackConstruction() {
    return {
        type: START_RACK_CONSTRUCTION,
    }
}

export function stopRackConstruction() {
    return {
        type: STOP_RACK_CONSTRUCTION,
    }
}

export function addRackToTile(positionX, positionY) {
    return (dispatch, getState) => {
        const { topology, interactionLevel } = getState()
        const currentRoom = topology.rooms[interactionLevel.roomId]
        const tiles = currentRoom.tiles.map((tileId) => topology.tiles[tileId])
        const tile = findTileWithPosition(tiles, positionX, positionY)

        if (tile !== null) {
            dispatch({
                type: ADD_RACK_TO_TILE,
                rack: {
                    id: uuid(),
                    name: 'Rack',
                    tileId: tile.id,
                    capacity: DEFAULT_RACK_SLOT_CAPACITY,
                    powerCapacityW: DEFAULT_RACK_POWER_CAPACITY,
                    machines: [],
                },
            })
        }
    }
}

export function deleteRoom(roomId) {
    return {
        type: DELETE_ROOM,
        roomId,
    }
}
