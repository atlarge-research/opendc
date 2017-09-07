import React from "react";
import FontAwesome from "react-fontawesome";

const EmptySlotComponent = ({position, onAdd}) => (
    <li className="list-group-item justify-content-between">
        <span className="badge badge-default badge-info">
            {position}
        </span>
        Add machine
        <button className="btn btn-secondary" onClick={onAdd}>
            <FontAwesome name="plus"/>
        </button>
    </li>
);

export default EmptySlotComponent;
