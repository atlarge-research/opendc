import React from "react";

const BackToBuildingComponent = ({onClick}) => (
    <div className="btn btn-secondary btn-block mb-2" onClick={onClick}>
        <span className="fa fa-angle-left mr-1"/>
        Back to building
    </div>
);

export default BackToBuildingComponent;
