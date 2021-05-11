import React from 'react'
import EmptySlotContainer from '../../../../../containers/app/sidebars/topology/rack/EmptySlotContainer'
import MachineContainer from '../../../../../containers/app/sidebars/topology/rack/MachineContainer'
import { machineList } from './MachineListComponent.module.scss'

const MachineListComponent = ({ machineIds }) => {
    return (
        <ul className={`list-group ${machineList}`}>
            {machineIds.map((machineId, index) => {
                if (machineId === null) {
                    return <EmptySlotContainer key={index} position={index + 1} />
                } else {
                    return <MachineContainer key={index} position={index + 1} machineId={machineId} />
                }
            })}
        </ul>
    )
}

export default MachineListComponent
