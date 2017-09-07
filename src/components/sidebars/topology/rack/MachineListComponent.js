import React from "react";
import EmptySlotContainer from "../../../../containers/sidebars/topology/rack/EmptySlotContainer";
import MachineContainer from "../../../../containers/sidebars/topology/rack/MachineContainer";
import "./MachineListComponent.css";

const MachineListComponent = ({machineIds}) => {
    return (
        <ul className="list-group machine-list">
            {machineIds.map((machineId, index) => {
                if (machineId === null) {
                    return <EmptySlotContainer key={index} position={index + 1}/>;
                } else {
                    return <MachineContainer key={index} position={index + 1} machineId={machineId}/>;
                }
            })}
        </ul>
    );
};

export default MachineListComponent;
