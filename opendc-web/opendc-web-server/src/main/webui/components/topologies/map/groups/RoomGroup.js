import PropTypes from 'prop-types'
import React from 'react'
import { Group } from 'react-konva'
import { InteractionLevel, Room } from '../../../../shapes'
import GrayContainer from '../GrayContainer'
import TileContainer from '../TileContainer'
import WallContainer from '../WallContainer'

function RoomGroup({ room, interactionLevel, currentRoomInConstruction, onClick }) {
    if (currentRoomInConstruction === room.id) {
        return (
            <Group onClick={onClick}>
                {room.tiles.map((tileId) => (
                    <TileContainer key={tileId} tileId={tileId} newTile={true} />
                ))}
            </Group>
        )
    }

    return (
        <Group onClick={onClick}>
            {(() => {
                if (
                    (interactionLevel.mode === 'RACK' || interactionLevel.mode === 'MACHINE') &&
                    interactionLevel.roomId === room.id
                ) {
                    return [
                        room.tiles
                            .filter((tileId) => tileId !== interactionLevel.tileId)
                            .map((tileId) => <TileContainer key={tileId} tileId={tileId} />),
                        <GrayContainer key={-1} />,
                        room.tiles
                            .filter((tileId) => tileId === interactionLevel.tileId)
                            .map((tileId) => <TileContainer key={tileId} tileId={tileId} />),
                    ]
                } else {
                    return room.tiles.map((tileId) => <TileContainer key={tileId} tileId={tileId} />)
                }
            })()}
            <WallContainer roomId={room.id} />
        </Group>
    )
}

RoomGroup.propTypes = {
    room: Room,
    interactionLevel: InteractionLevel,
    currentRoomInConstruction: PropTypes.string,
    onClick: PropTypes.func,
}

export default RoomGroup
