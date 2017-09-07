import React from "react";
import BackToRackContainer from "../../../../containers/sidebars/topology/machine/BackToRackContainer";
import DeleteMachineContainer from "../../../../containers/sidebars/topology/machine/DeleteMachineContainer";
import MachineNameContainer from "../../../../containers/sidebars/topology/machine/MachineNameContainer";

const MachineSidebarComponent = () => {
    return (
        <div>
            <MachineNameContainer/>
            <BackToRackContainer/>
            <DeleteMachineContainer/>
        </div>
    );
};

export default MachineSidebarComponent;
