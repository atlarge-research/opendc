import React from 'react'
import { useSelector } from 'react-redux'
import MachineListComponent from '../../../../../components/app/sidebars/topology/rack/MachineListComponent'

const MachineListContainer = (props) => {
    const machineIds = useSelector(
        (state) => state.objects.rack[state.objects.tile[state.interactionLevel.tileId].rackId].machineIds
    )
    return <MachineListComponent {...props} machineIds={machineIds} />
}

export default MachineListContainer
