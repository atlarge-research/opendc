import PropTypes from 'prop-types'
import React from 'react'
import { Group } from 'react-konva'
import { Tile } from '../../../../shapes'
import { ROOM_DEFAULT_COLOR, ROOM_IN_CONSTRUCTION_COLOR } from '../../../../util/colors'
import RoomTile from '../elements/RoomTile'
import RackContainer from '../RackContainer'

function TileGroup({ tile, newTile, onClick }) {
    let tileObject
    if (tile.rack) {
        tileObject = <RackContainer tile={tile} />
    } else {
        tileObject = null
    }

    let color = ROOM_DEFAULT_COLOR
    if (newTile) {
        color = ROOM_IN_CONSTRUCTION_COLOR
    }

    return (
        <Group onClick={() => onClick(tile)}>
            <RoomTile tile={tile} color={color} />
            {tileObject}
        </Group>
    )
}

TileGroup.propTypes = {
    tile: Tile,
    newTile: PropTypes.bool,
    onClick: PropTypes.func,
}

export default TileGroup
