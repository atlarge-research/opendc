import React from "react";

const NewRoomConstructionComponent = ({onStart, onFinish, onCancel, currentRoomInConstruction}) => {
    if (currentRoomInConstruction === -1) {
        return (
            <div className="btn btn-primary btn-block" onClick={onStart}>
                Construct a new room
            </div>
        );
    }
    return (
        <div>
            <div className="btn btn-primary btn-block" onClick={onFinish}>
                Finalize new room
            </div>
            <div className="btn btn-default btn-block" onClick={onCancel}>
                Cancel construction
            </div>
        </div>
    );

};

export default NewRoomConstructionComponent;
