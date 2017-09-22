import React from "react";

const BackToRackComponent = ({onClick}) => (
    <div className="btn btn-secondary btn-block" onClick={onClick}>
        <span className="fa fa-angle-left mr-2"/>
        Back to rack
    </div>
);

export default BackToRackComponent;
