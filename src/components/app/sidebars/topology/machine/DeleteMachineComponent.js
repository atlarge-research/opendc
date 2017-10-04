import React from "react";

const DeleteMachineComponent = ({ onClick }) => (
  <div className="btn btn-danger btn-block" onClick={onClick}>
    <span className="fa fa-trash mr-2" />
    Delete this machine
  </div>
);

export default DeleteMachineComponent;
