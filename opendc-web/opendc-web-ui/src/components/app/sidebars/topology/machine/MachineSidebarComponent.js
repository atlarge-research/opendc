import PropTypes from 'prop-types'
import React from 'react'
import BackToRackContainer from '../../../../../containers/app/sidebars/topology/machine/BackToRackContainer'
import DeleteMachineContainer from '../../../../../containers/app/sidebars/topology/machine/DeleteMachineContainer'
import MachineNameContainer from '../../../../../containers/app/sidebars/topology/machine/MachineNameContainer'
import UnitTabsContainer from '../../../../../containers/app/sidebars/topology/machine/UnitTabsContainer'

const MachineSidebarComponent = ({ machineId }) => {
    return (
        <div className="h-100 overflow-auto">
            <MachineNameContainer />
            <BackToRackContainer />
            <DeleteMachineContainer />
            <UnitTabsContainer />
        </div>
    )
}

MachineSidebarComponent.propTypes = {
    machineId: PropTypes.string,
}

export default MachineSidebarComponent
