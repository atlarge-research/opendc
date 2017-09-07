import React from "react";
import DeleteRackContainer from "../../../../containers/sidebars/topology/rack/DeleteRackContainer";
import MachineListContainer from "../../../../containers/sidebars/topology/rack/MachineListContainer";
import RackNameContainer from "../../../../containers/sidebars/topology/rack/RackNameContainer";

const RackSidebarComponent = () => {
    return (
        <div>
            <RackNameContainer/>
            <DeleteRackContainer/>
            <MachineListContainer/>
        </div>
    );
};

export default RackSidebarComponent;
