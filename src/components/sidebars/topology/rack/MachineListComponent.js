import React from "react";
import EmptySlotContainer from "../../../../containers/sidebars/topology/rack/EmptySlotContainer";
import MachineContainer from "../../../../containers/sidebars/topology/rack/MachineContainer";

const MachineListComponent = ({machineIds}) => {
    return (
        <ul className="list-group">
            {machineIds.map((machineId, index) => {
                if (machineId === null) {
                    return <EmptySlotContainer key={index} position={index}/>;
                } else {
                    return <MachineContainer key={index} position={index} machineId={machineId}/>;
                }
            })}
        </ul>
    );
};

export default MachineListComponent;
