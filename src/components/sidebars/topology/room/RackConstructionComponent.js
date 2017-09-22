import React from "react";

const RackConstructionComponent = ({inRackConstructionMode, onStart, onStop}) => {
    if (inRackConstructionMode) {
        return (
            <div className="btn btn-primary btn-block" onClick={onStop}>
                <span className="fa fa-times mr-2"/>
                Stop rack construction
            </div>
        );
    }

    return (
        <div className="btn btn-primary btn-block" onClick={onStart}>
            <span className="fa fa-plus mr-2"/>
            Start rack construction
        </div>
    );
};

export default RackConstructionComponent;
