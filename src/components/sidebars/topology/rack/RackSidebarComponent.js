import React from "react";
import DeleteRackContainer from "../../../../containers/sidebars/topology/rack/DeleteRackContainer";
import MachineListContainer from "../../../../containers/sidebars/topology/rack/MachineListContainer";
import RackNameContainer from "../../../../containers/sidebars/topology/rack/RackNameContainer";
import "./RackSidebarComponent.css";

const RackSidebarComponent = () => {
    return (
        <div className="rack-sidebar-container flex-column">
            <RackNameContainer/>
            <DeleteRackContainer/>
            <div className="machine-list-container mt-2">
                <MachineListContainer/>
            </div>
        </div>
    );
};

export default RackSidebarComponent;
