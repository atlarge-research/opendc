import React from "react";
import CancelNewRoomConstructionButton from "../../../../containers/sidebars/topology/building/CancelNewRoomConstructionButton";
import FinishNewRoomConstructionButton from "../../../../containers/sidebars/topology/building/FinishNewRoomConstructionButton";
import StartNewRoomConstructionButton from "../../../../containers/sidebars/topology/building/StartNewRoomConstructionButton";

const BuildingSidebarContentComponent = ({currentRoomInConstruction}) => {
    if (currentRoomInConstruction !== -1) {
        return (
            <div>
                <FinishNewRoomConstructionButton/>
                <CancelNewRoomConstructionButton/>
            </div>
        );
    }
    return (
        <StartNewRoomConstructionButton/>
    );
};

export default BuildingSidebarContentComponent;
