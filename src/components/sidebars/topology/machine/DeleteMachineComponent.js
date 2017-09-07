import React from "react";

const DeleteMachineComponent = ({onClick}) => {
    return (
        <div className="btn btn-danger btn-block" onClick={onClick}>
            Delete this machine
        </div>
    );
};

export default DeleteMachineComponent;
