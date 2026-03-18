import { v4 as uuid } from 'uuid'
import { normalize } from 'normalizr'
import { Rack as RackSchema } from '../../../util/topology-schema'
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

export function startRackConstruction(rackPrefab) {
    return {
        type: START_RACK_CONSTRUCTION,
        rackPrefab,
    }
}

export function stopRackConstruction() {
    return {
        type: STOP_RACK_CONSTRUCTION,
    }
}

export function addRackToTile(positionX, positionY) {
    return (dispatch, getState) => {
        const { topology, interactionLevel, construction } = getState()
        const currentRoom = topology.rooms[interactionLevel.roomId]
        const tiles = currentRoom.tiles.map((tileId) => topology.tiles[tileId])
        const tile = findTileWithPosition(tiles, positionX, positionY)

        if (tile !== null) {
            const prefab = construction.currentRackPrefab
            const rackId = uuid()
            const rack = prefab
                ? {
                      ...prefab.rack,
                      id: rackId,
                      machines: (prefab.rack.machines || []).map((m) => ({ ...m, id: uuid(), rackId })),
                  }
                : {
                      id: rackId,
                      name: 'Rack',
                      capacity: DEFAULT_RACK_SLOT_CAPACITY,
                      powerCapacityW: DEFAULT_RACK_POWER_CAPACITY,
                      machines: [],
                  }

            const { entities, result: normalizedRackId } = normalize(rack, RackSchema)
            dispatch({
                type: ADD_RACK_TO_TILE,
                tileId: tile.id,
                rack: entities.racks[normalizedRackId],
                entities,
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
