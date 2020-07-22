import React from 'react'
import { Group } from 'react-konva'
import RackEnergyFillContainer from '../../../../containers/app/map/RackEnergyFillContainer'
import RackSpaceFillContainer from '../../../../containers/app/map/RackSpaceFillContainer'
import Shapes from '../../../../shapes/index'
import { RACK_BACKGROUND_COLOR } from '../../../../util/colors'
import TileObject from '../elements/TileObject'

const RackGroup = ({ tile }) => {
    return (
        <Group>
            <TileObject positionX={tile.positionX} positionY={tile.positionY} color={RACK_BACKGROUND_COLOR} />
            <Group>
                <RackSpaceFillContainer tileId={tile._id} positionX={tile.positionX} positionY={tile.positionY} />
                <RackEnergyFillContainer tileId={tile._id} positionX={tile.positionX} positionY={tile.positionY} />
            </Group>
        </Group>
    )
}

RackGroup.propTypes = {
    tile: Shapes.Tile,
}

export default RackGroup
