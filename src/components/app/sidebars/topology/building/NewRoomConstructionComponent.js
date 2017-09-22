import React from "react";

const NewRoomConstructionComponent = ({onStart, onFinish, onCancel, currentRoomInConstruction}) => {
    if (currentRoomInConstruction === -1) {
        return (
            <div className="btn btn-primary btn-block" onClick={onStart}>
                <span className="fa fa-plus mr-2"/>
                Construct a new room
            </div>
        );
    }
    return (
        <div>
            <div className="btn btn-primary btn-block" onClick={onFinish}>
                <span className="fa fa-check mr-2"/>
                Finalize new room
            </div>
            <div className="btn btn-default btn-block" onClick={onCancel}>
                <span className="fa fa-times mr-2"/>
                Cancel construction
            </div>
        </div>
    );

};

export default NewRoomConstructionComponent;
