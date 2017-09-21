import React from "react";

const UnitComponent = ({unit, onDelete, inSimulation}) => (
    <li className="d-flex list-group-item justify-content-between align-items-center">
        {unit.manufacturer + " " + unit.family + " " + unit.model + " " + unit.generation}
        {inSimulation ?
            undefined :
            <span className="btn btn-outline-danger" onClick={onDelete}>Delete</span>
        }
    </li>
);

export default UnitComponent;
