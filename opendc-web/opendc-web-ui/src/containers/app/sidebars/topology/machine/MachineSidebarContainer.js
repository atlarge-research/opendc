import React from 'react'
import { useSelector } from 'react-redux'
import MachineSidebarComponent from '../../../../../components/app/sidebars/topology/machine/MachineSidebarComponent'

const MachineSidebarContainer = (props) => {
    const machineId = useSelector(
        (state) =>
            state.objects.rack[state.objects.tile[state.interactionLevel.tileId].rackId].machineIds[
                state.interactionLevel.position - 1
            ]
    )
    return <MachineSidebarComponent {...props} machineId={machineId} />
}

export default MachineSidebarContainer
