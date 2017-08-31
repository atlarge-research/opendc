import React from "react";
import CancelNewRoomConstructionButton from "../../../../containers/sidebars/topology/building/CancelNewRoomConstructionButton";
import FinishNewRoomConstructionButton from "../../../../containers/sidebars/topology/building/FinishNewRoomConstructionButton";
import StartNewRoomConstructionButton from "../../../../containers/sidebars/topology/building/StartNewRoomConstructionButton";

const BuildingSidebarComponent = ({currentRoomInConstruction}) => {
    return (
        <div>
            <h2>Building</h2>
            {currentRoomInConstruction === -1 ?
                <StartNewRoomConstructionButton/> :
                <div>
                    <FinishNewRoomConstructionButton/>
                    <CancelNewRoomConstructionButton/>
                </div>
            }
        </div>
    );
};

export default BuildingSidebarComponent;
