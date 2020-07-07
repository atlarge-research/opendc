import PropTypes from 'prop-types'
import React from 'react'
import { Group } from 'react-konva'
import RackContainer from '../../../../containers/app/map/RackContainer'
import Shapes from '../../../../shapes/index'
import { ROOM_DEFAULT_COLOR, ROOM_IN_CONSTRUCTION_COLOR } from '../../../../util/colors'
import RoomTile from '../elements/RoomTile'

const TileGroup = ({ tile, newTile, roomLoad, onClick }) => {
    let tileObject
    if (tile.rackId) {
        tileObject = <RackContainer tile={tile}/>
    } else {
        tileObject = null
    }

    let color = ROOM_DEFAULT_COLOR
    if (newTile) {
        color = ROOM_IN_CONSTRUCTION_COLOR
    }

    return (
        <Group onClick={() => onClick(tile)}>
            <RoomTile tile={tile} color={color}/>
            {tileObject}
        </Group>
    )
}

TileGroup.propTypes = {
    tile: Shapes.Tile,
    newTile: PropTypes.bool,
}

export default TileGroup
