import React from 'react'
import { useSelector } from 'react-redux'
import TopologyGroup from '../../../components/app/map/groups/TopologyGroup'
import { useActiveTopology } from '../../../data/topology'

const TopologyContainer = () => {
    const topology = useActiveTopology()
    const interactionLevel = useSelector((state) => state.interactionLevel)

    return <TopologyGroup topology={topology} interactionLevel={interactionLevel} />
}

export default TopologyContainer
