import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { addRackToTile } from '../../../../redux/actions/topology/room'
import ObjectHoverLayerComponent from '../../../../components/app/map/layers/ObjectHoverLayerComponent'
import { findTileWithPosition } from '../../../../util/tile-calculations'

const ObjectHoverLayer = (props) => {
    const state = useSelector((state) => {
        return {
            mapPosition: state.map.position,
            mapScale: state.map.scale,
            isEnabled: () => state.construction.inRackConstructionMode,
            isValid: (x, y) => {
                if (state.interactionLevel.mode !== 'ROOM') {
                    return false
                }

                const currentRoom = state.objects.room[state.interactionLevel.roomId]
                const tiles = currentRoom.tileIds.map((tileId) => state.objects.tile[tileId])
                const tile = findTileWithPosition(tiles, x, y)

                return !(tile === null || tile.rackId)
            },
        }
    })

    const dispatch = useDispatch()
    const onClick = (x, y) => dispatch(addRackToTile(x, y))
    return <ObjectHoverLayerComponent {...props} {...state} onClick={onClick} />
}

export default ObjectHoverLayer
