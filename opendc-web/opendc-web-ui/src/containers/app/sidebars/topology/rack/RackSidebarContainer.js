import React from 'react'
import { useSelector } from 'react-redux'
import RackSidebarComponent from '../../../../../components/app/sidebars/topology/rack/RackSidebarComponent'

const RackSidebarContainer = (props) => {
    const rackId = useSelector((state) => state.objects.tile[state.interactionLevel.tileId].rack)
    return <RackSidebarComponent {...props} rackId={rackId} />
}

export default RackSidebarContainer
