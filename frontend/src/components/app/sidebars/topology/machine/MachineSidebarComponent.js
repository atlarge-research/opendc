import React from 'react'
import LoadBarContainer from '../../../../../containers/app/sidebars/elements/LoadBarContainer'
import LoadChartContainer from '../../../../../containers/app/sidebars/elements/LoadChartContainer'
import BackToRackContainer from '../../../../../containers/app/sidebars/topology/machine/BackToRackContainer'
import DeleteMachineContainer from '../../../../../containers/app/sidebars/topology/machine/DeleteMachineContainer'
import MachineNameContainer from '../../../../../containers/app/sidebars/topology/machine/MachineNameContainer'
import UnitTabsContainer from '../../../../../containers/app/sidebars/topology/machine/UnitTabsContainer'

const MachineSidebarComponent = ({ inSimulation, machineId }) => {
    return (
        <div>
            <MachineNameContainer/>
            <BackToRackContainer/>
            {inSimulation ? (
                <div>
                    <LoadBarContainer objectType="machine" objectId={machineId}/>
                    <LoadChartContainer objectType="machine" objectId={machineId}/>
                </div>
            ) : (
                <DeleteMachineContainer/>
            )}
            <UnitTabsContainer/>
        </div>
    )
}

export default MachineSidebarComponent
