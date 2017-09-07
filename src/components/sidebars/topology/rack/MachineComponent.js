import React from "react";
import Shapes from "../../../../shapes";

const MachineComponent = ({position, machine, onClick}) => (
    <li className="list-group-item list-group-item-action justify-content-between" onClick={onClick}>
        <span className="badge badge-default badge-info">
            {position}
        </span>
        <span className="badge badge-primary badge-pill">
            {machine.cpuIds.length} CPUs
        </span>
        <span className="badge badge-warning badge-pill">
            {machine.gpuIds.length} GPUs
        </span>
        <span className="badge badge-success badge-pill">
            {machine.memoryIds.length} Memories
        </span>
        <span className="badge badge-info badge-pill">
            {machine.storageIds.length} Storages
        </span>
    </li>
);

MachineComponent.propTypes = {
    machine: Shapes.Machine
};

export default MachineComponent;
