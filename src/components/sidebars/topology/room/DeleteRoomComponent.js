import React from "react";

const DeleteRoomComponent = ({onClick}) => {
    return (
        <div className="btn btn-danger btn-block" onClick={onClick}>
            <span className="fa fa-trash mr-1"/>
            Delete this room
        </div>
    );
};

export default DeleteRoomComponent;
