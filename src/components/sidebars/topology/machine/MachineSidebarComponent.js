import React from "react";
import BackToRackContainer from "../../../../containers/sidebars/topology/machine/BackToRackContainer";
import DeleteMachineContainer from "../../../../containers/sidebars/topology/machine/DeleteMachineContainer";
import MachineNameContainer from "../../../../containers/sidebars/topology/machine/MachineNameContainer";
import UnitTabsComponent from "./UnitTabsComponent";

const MachineSidebarComponent = () => {
    return (
        <div>
            <MachineNameContainer/>
            <BackToRackContainer/>
            <DeleteMachineContainer/>
            <UnitTabsComponent/>
        </div>
    );
};

export default MachineSidebarComponent;
