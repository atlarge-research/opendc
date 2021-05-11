import React from 'react'
import BackToRoomContainer from '../../../../../containers/app/sidebars/topology/rack/BackToRoomContainer'
import DeleteRackContainer from '../../../../../containers/app/sidebars/topology/rack/DeleteRackContainer'
import MachineListContainer from '../../../../../containers/app/sidebars/topology/rack/MachineListContainer'
import RackNameContainer from '../../../../../containers/app/sidebars/topology/rack/RackNameContainer'
import { sidebarContainer, sidebarHeaderContainer, machineListContainer } from './RackSidebarComponent.module.scss'
import AddPrefabContainer from '../../../../../containers/app/sidebars/topology/rack/AddPrefabContainer'

const RackSidebarComponent = () => {
    return (
        <div className={`${sidebarContainer} flex-column`}>
            <div className={sidebarHeaderContainer}>
                <RackNameContainer />
                <BackToRoomContainer />
                <AddPrefabContainer />
                <DeleteRackContainer />
            </div>
            <div className={`${machineListContainer} mt-2`}>
                <MachineListContainer />
            </div>
        </div>
    )
}

export default RackSidebarComponent
