import React from 'react'
import { Group } from 'react-konva'
import GrayContainer from '../../../../containers/app/map/GrayContainer'
import RoomContainer from '../../../../containers/app/map/RoomContainer'
import { InteractionLevel, Topology } from '../../../../shapes'

const TopologyGroup = ({ topology, interactionLevel }) => {
    if (!topology) {
        return <Group />
    }

    if (interactionLevel.mode === 'BUILDING') {
        return (
            <Group>
                {topology.rooms.map((roomId) => (
                    <RoomContainer key={roomId} roomId={roomId} />
                ))}
            </Group>
        )
    }

    return (
        <Group>
            {topology.rooms
                .filter((roomId) => roomId !== interactionLevel.roomId)
                .map((roomId) => (
                    <RoomContainer key={roomId} roomId={roomId} />
                ))}
            {interactionLevel.mode === 'ROOM' ? <GrayContainer /> : null}
            {topology.rooms
                .filter((roomId) => roomId === interactionLevel.roomId)
                .map((roomId) => (
                    <RoomContainer key={roomId} roomId={roomId} />
                ))}
        </Group>
    )
}

TopologyGroup.propTypes = {
    topology: Topology,
    interactionLevel: InteractionLevel,
}

export default TopologyGroup
