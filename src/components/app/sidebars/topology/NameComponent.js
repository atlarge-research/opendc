import React from "react";
import FontAwesome from "react-fontawesome";

const NameComponent = ({name, onEdit}) => (
    <h2>
        {name}
        <button className="btn btn-outline-secondary float-right" onClick={onEdit}>
            <FontAwesome name="pencil"/>
        </button>
    </h2>
);

export default NameComponent;
