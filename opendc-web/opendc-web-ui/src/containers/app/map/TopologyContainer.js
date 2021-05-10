import React from 'react'
import { useSelector } from 'react-redux'
import TopologyGroup from '../../../components/app/map/groups/TopologyGroup'

const TopologyContainer = () => {
    const topology = useSelector(
        (state) => state.currentTopologyId !== '-1' && state.objects.topology[state.currentTopologyId]
    )
    const interactionLevel = useSelector((state) => state.interactionLevel)

    return <TopologyGroup topology={topology} interactionLevel={interactionLevel} />
}

export default TopologyContainer
