import React from 'react'
import { useSelector } from 'react-redux'
import TopologySidebarComponent from '../../../../components/app/sidebars/topology/TopologySidebarComponent'

const TopologySidebarContainer = (props) => {
    const interactionLevel = useSelector((state) => state.interactionLevel)
    return <TopologySidebarComponent {...props} interactionLevel={interactionLevel} />
}

export default TopologySidebarContainer
