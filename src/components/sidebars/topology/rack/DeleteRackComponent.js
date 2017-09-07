import React from "react";

const DeleteRackComponent = ({onClick}) => {
    return (
        <div className="btn btn-danger btn-block" onClick={onClick}>
            Delete this rack
        </div>
    );
};

export default DeleteRackComponent;
