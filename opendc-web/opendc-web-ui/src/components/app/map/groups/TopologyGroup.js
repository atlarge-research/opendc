import React from 'react'
import { Group } from 'react-konva'
import GrayContainer from '../../../../containers/app/map/GrayContainer'
import RoomContainer from '../../../../containers/app/map/RoomContainer'
import Shapes from '../../../../shapes/index'

const TopologyGroup = ({ topology, interactionLevel }) => {
    if (!topology) {
        return <Group />
    }

    if (interactionLevel.mode === 'BUILDING') {
        return (
            <Group>
                {topology.roomIds.map((roomId) => (
                    <RoomContainer key={roomId} roomId={roomId} />
                ))}
            </Group>
        )
    }

    return (
        <Group>
            {topology.roomIds
                .filter((roomId) => roomId !== interactionLevel.roomId)
                .map((roomId) => (
                    <RoomContainer key={roomId} roomId={roomId} />
                ))}
            {interactionLevel.mode === 'ROOM' ? <GrayContainer /> : null}
            {topology.roomIds
                .filter((roomId) => roomId === interactionLevel.roomId)
                .map((roomId) => (
                    <RoomContainer key={roomId} roomId={roomId} />
                ))}
        </Group>
    )
}

TopologyGroup.propTypes = {
    topology: Shapes.Topology,
    interactionLevel: Shapes.InteractionLevel,
}

export default TopologyGroup
