import React from 'react'
import { Group } from 'react-konva'
import { InteractionLevel, Topology } from '../../../../shapes'
import DatacenterContainer from '../DatacenterContainer'
import GrayContainer from '../GrayContainer'

function TopologyGroup({ topology, interactionLevel }) {
    if (!topology) {
        return <Group />
    }

    if (interactionLevel.mode === 'BUILDING') {
        return (
            <Group>
                {topology.datacenters.map((datacenterId) => (
                    <DatacenterContainer key={datacenterId} datacenterId={datacenterId} />
                ))}
            </Group>
        )
    }

    return (
        <Group>
            {topology.datacenters
                .filter((datacenterId) => datacenterId !== interactionLevel.datacenterId)
                .map((datacenterId) => (
                    <DatacenterContainer key={datacenterId} datacenterId={datacenterId} />
                ))}
            {interactionLevel.mode === 'DATACENTER' ? <GrayContainer /> : null}
            {topology.datacenters
                .filter((datacenterId) => datacenterId === interactionLevel.datacenterId)
                .map((datacenterId) => (
                    <DatacenterContainer key={datacenterId} datacenterId={datacenterId} />
                ))}
        </Group>
    )
}

TopologyGroup.propTypes = {
    topology: Topology,
    interactionLevel: InteractionLevel,
}

export default TopologyGroup
