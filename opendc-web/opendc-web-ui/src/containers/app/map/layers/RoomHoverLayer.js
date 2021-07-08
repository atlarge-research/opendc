import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { toggleTileAtLocation } from '../../../../redux/actions/topology/building'
import RoomHoverLayerComponent from '../../../../components/app/map/layers/RoomHoverLayerComponent'
import {
    deriveValidNextTilePositions,
    findPositionInPositions,
    findPositionInRooms,
} from '../../../../util/tile-calculations'

const RoomHoverLayer = (props) => {
    const dispatch = useDispatch()
    const onClick = (x, y) => dispatch(toggleTileAtLocation(x, y))

    const state = useSelector((state) => {
        return {
            mapPosition: state.map.position,
            mapScale: state.map.scale,
            isEnabled: () => state.construction.currentRoomInConstruction !== '-1',
            isValid: (x, y) => {
                const newRoom = Object.assign({}, state.objects.room[state.construction.currentRoomInConstruction])
                const oldRooms = Object.keys(state.objects.room)
                    .map((id) => Object.assign({}, state.objects.room[id]))
                    .filter(
                        (room) =>
                            state.objects.topology[state.currentTopologyId].rooms.indexOf(room._id) !== -1 &&
                            room._id !== state.construction.currentRoomInConstruction
                    )

                ;[...oldRooms, newRoom].forEach((room) => {
                    room.tiles = room.tiles.map((tileId) => state.objects.tile[tileId])
                })
                if (newRoom.tiles.length === 0) {
                    return findPositionInRooms(oldRooms, x, y) === -1
                }

                const validNextPositions = deriveValidNextTilePositions(oldRooms, newRoom.tiles)
                return findPositionInPositions(validNextPositions, x, y) !== -1
            },
        }
    })
    return <RoomHoverLayerComponent onClick={onClick} {...props} {...state} />
}

export default RoomHoverLayer
