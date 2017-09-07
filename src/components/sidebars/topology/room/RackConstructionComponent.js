import React from "react";

const RackConstructionComponent = ({inRackConstructionMode, onStart, onStop}) => {
    if (inRackConstructionMode) {
        return (
            <div className="btn btn-primary btn-block" onClick={onStop}>
                Stop rack construction
            </div>
        );
    }

    return (
        <div className="btn btn-primary btn-block" onClick={onStart}>
            Start rack construction
        </div>
    );
};

export default RackConstructionComponent;
