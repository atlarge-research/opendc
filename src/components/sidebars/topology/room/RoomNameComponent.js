import React from "react";
import FontAwesome from "react-fontawesome";

const RoomNameComponent = ({roomName, onEdit}) => (
    <h2>
        {roomName}
        <button className="btn btn-outline-secondary float-right" onClick={onEdit}>
            <FontAwesome name="pencil"/>
        </button>
    </h2>
);

export default RoomNameComponent;
