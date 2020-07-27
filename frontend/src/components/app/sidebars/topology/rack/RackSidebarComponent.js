import React from 'react'
import BackToRoomContainer from '../../../../../containers/app/sidebars/topology/rack/BackToRoomContainer'
import DeleteRackContainer from '../../../../../containers/app/sidebars/topology/rack/DeleteRackContainer'
import MachineListContainer from '../../../../../containers/app/sidebars/topology/rack/MachineListContainer'
import RackNameContainer from '../../../../../containers/app/sidebars/topology/rack/RackNameContainer'
import './RackSidebarComponent.css'
import AddPrefabContainer from "../../../../../containers/app/sidebars/topology/rack/AddPrefabContainer";

const RackSidebarComponent = () => {
    return (
        <div className="rack-sidebar-container flex-column">
            <div className="rack-sidebar-header-container">
                <RackNameContainer />
                <BackToRoomContainer />
                <AddPrefabContainer />
                <DeleteRackContainer />
            </div>
            <div className="machine-list-container mt-2">
                <MachineListContainer />
            </div>
        </div>
    )
}

export default RackSidebarComponent
