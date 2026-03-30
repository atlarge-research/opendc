import React from 'react'
import { Group } from 'react-konva'
import { InteractionLevel, Topology } from '../../../../shapes'
import RoomContainer from '../RoomContainer'
import GrayContainer from '../GrayContainer'

function TopologyGroup({ topology, interactionLevel }) {
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
