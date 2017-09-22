import React from "react";
import LoadBarContainer from "../../../../containers/sidebars/elements/LoadBarContainer";
import LoadChartContainer from "../../../../containers/sidebars/elements/LoadChartContainer";
import BackToBuildingContainer from "../../../../containers/sidebars/topology/room/BackToBuildingContainer";
import DeleteRoomContainer from "../../../../containers/sidebars/topology/room/DeleteRoomContainer";
import RackConstructionContainer from "../../../../containers/sidebars/topology/room/RackConstructionContainer";
import RoomNameContainer from "../../../../containers/sidebars/topology/room/RoomNameContainer";
import RoomTypeContainer from "../../../../containers/sidebars/topology/room/RoomTypeContainer";

const RoomSidebarComponent = ({roomId, roomType, inSimulation}) => {
    let allowedObjects;
    if (!inSimulation && roomType === "SERVER") {
        allowedObjects = <RackConstructionContainer/>;
    }

    return (
        <div>
            <RoomNameContainer/>
            <RoomTypeContainer/>
            <BackToBuildingContainer/>
            {inSimulation ?
                <div>
                    <LoadBarContainer objectType="room" objectId={roomId}/>
                    <LoadChartContainer objectType="room" objectId={roomId}/>
                </div> :
                <div>
                    {allowedObjects}
                    <DeleteRoomContainer/>
                </div>
            }
        </div>
    );
};

export default RoomSidebarComponent;
