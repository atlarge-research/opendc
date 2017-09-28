import React from "react";
import Shapes from "../../../../../shapes";
import {convertLoadToSimulationColor} from "../../../../../util/simulation-load";

const UnitIcon = ({id, type}) => (
    <div>
        <img
            src={"/img/topology/" + id + "-icon.png"}
            alt={"Machine contains " + type + " units"}
            className="img-fluid ml-1"
            style={{maxHeight: "35px"}}
        />
    </div>
);

const MachineComponent = ({position, machine, inSimulation, machineLoad, onClick}) => {
    let color = "white";
    if (inSimulation && machineLoad >= 0) {
        color = convertLoadToSimulationColor(machineLoad);
    }
    const hasNoUnits = machine.cpuIds.length + machine.gpuIds.length + machine.memoryIds.length
        + machine.storageIds.length === 0;

    return (
        <li
            className="d-flex list-group-item list-group-item-action justify-content-between align-items-center"
            onClick={onClick}
            style={{backgroundColor: color}}
        >
            <span className="badge badge-default badge-info mr-1">
                {position}
            </span>
            <div className="d-inline-flex">
                {machine.cpuIds.length > 0 ?
                    <UnitIcon id="cpu" type="CPU"/> :
                    undefined
                }
                {machine.gpuIds.length > 0 ?
                    <UnitIcon id="gpu" type="GPU"/> :
                    undefined
                }
                {machine.memoryIds.length > 0 ?
                    <UnitIcon id="memory" type="memory"/> :
                    undefined
                }
                {machine.storageIds.length > 0 ?
                    <UnitIcon id="storage" type="storage"/> :
                    undefined
                }
                {hasNoUnits ?
                    <span className="badge badge-default badge-warning">
                        Machine with no units
                    </span> :
                    undefined
                }
            </div>
        </li>
    );
};

MachineComponent.propTypes = {
    machine: Shapes.Machine
};

export default MachineComponent;
