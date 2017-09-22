import React from "react";

const DeleteMachineComponent = ({onClick}) => {
    return (
        <div className="btn btn-danger btn-block" onClick={onClick}>
            <span className="fa fa-trash mr-1"/>
            Delete this machine
        </div>
    );
};

export default DeleteMachineComponent;
