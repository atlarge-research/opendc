import React from "react";
import LoadBarContainer from "../../../../containers/sidebars/elements/LoadBarContainer";
import LoadChartContainer from "../../../../containers/sidebars/elements/LoadChartContainer";
import BackToRackContainer from "../../../../containers/sidebars/topology/machine/BackToRackContainer";
import DeleteMachineContainer from "../../../../containers/sidebars/topology/machine/DeleteMachineContainer";
import MachineNameContainer from "../../../../containers/sidebars/topology/machine/MachineNameContainer";
import UnitTabsContainer from "../../../../containers/sidebars/topology/machine/UnitTabsContainer";

const MachineSidebarComponent = ({inSimulation, machineId}) => {
    return (
        <div>
            <MachineNameContainer/>
            <BackToRackContainer/>
            {inSimulation ?
                <div>
                    <LoadBarContainer objectType="machine" objectId={machineId}/>
                    <LoadChartContainer objectType="machine" objectId={machineId}/>
                </div> :
                <DeleteMachineContainer/>
            }
            <UnitTabsContainer/>
        </div>
    );
};

export default MachineSidebarComponent;
