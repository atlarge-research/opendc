import React from "react";

const UnitComponent = ({unit, onDelete}) => (
    <li className="d-flex list-group-item justify-content-between align-items-center">
        {unit.manufacturer + " " + unit.family + " " + unit.model + " " + unit.generation}
        <span className="btn btn-outline-danger" onClick={onDelete}>Delete</span>
    </li>
);

export default UnitComponent;
