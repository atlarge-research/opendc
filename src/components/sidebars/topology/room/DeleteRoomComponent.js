import React from "react";

const DeleteRoomComponent = ({onClick}) => {
    return (
        <div className="btn btn-danger btn-block" onClick={onClick}>
            Delete this room
        </div>
    );
};

export default DeleteRoomComponent;
