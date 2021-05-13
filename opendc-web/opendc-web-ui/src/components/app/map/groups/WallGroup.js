import PropTypes from 'prop-types'
import React from 'react'
import { Group } from 'react-konva'
import { Tile } from '../../../../shapes/index'
import { deriveWallLocations } from '../../../../util/tile-calculations'
import WallSegment from '../elements/WallSegment'

const WallGroup = ({ tiles }) => {
    return (
        <Group>
            {deriveWallLocations(tiles).map((wallSegment, index) => (
                <WallSegment key={index} wallSegment={wallSegment} />
            ))}
        </Group>
    )
}

WallGroup.propTypes = {
    tiles: PropTypes.arrayOf(Tile).isRequired,
}

export default WallGroup
