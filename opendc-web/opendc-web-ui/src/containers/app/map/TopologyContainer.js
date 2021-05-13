import React from 'react'
import { useSelector } from 'react-redux'
import TopologyGroup from '../../../components/app/map/groups/TopologyGroup'
import { useTopology } from '../../../store/hooks/topology'

const TopologyContainer = () => {
    const topology = useTopology()
    const interactionLevel = useSelector((state) => state.interactionLevel)

    return <TopologyGroup topology={topology} interactionLevel={interactionLevel} />
}

export default TopologyContainer
