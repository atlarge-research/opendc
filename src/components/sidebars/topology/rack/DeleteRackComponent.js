import React from "react";

const DeleteRackComponent = ({onClick}) => {
    return (
        <div className="btn btn-danger btn-block" onClick={onClick}>
            <span className="fa fa-trash mr-1"/>
            Delete this rack
        </div>
    );
};

export default DeleteRackComponent;
