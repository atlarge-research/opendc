import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { goFromRoomToRack } from '../../../actions/interaction-level'
import TileGroup from '../../../components/app/map/groups/TileGroup'

const TileContainer = (props) => {
    const interactionLevel = useSelector((state) => state.interactionLevel)
    const tile = useSelector((state) => state.objects.tile[props.tileId])

    const dispatch = useDispatch()
    const onClick = (tile) => {
        if (tile.rackId) {
            dispatch(goFromRoomToRack(tile._id))
        }
    }
    return <TileGroup {...props} onClick={onClick} tile={tile} interactionLevel={interactionLevel} />
}

export default TileContainer
