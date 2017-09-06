import React from "react";
import DeleteRoomContainer from "../../../../containers/sidebars/topology/room/DeleteRoomContainer";
import RackConstructionContainer from "../../../../containers/sidebars/topology/room/RackConstructionContainer";
import RoomNameContainer from "../../../../containers/sidebars/topology/room/RoomNameContainer";
import RoomTypeContainer from "../../../../containers/sidebars/topology/room/RoomTypeContainer";

const RoomSidebarComponent = ({roomType}) => {
    let allowedObjects;
    if (roomType === "SERVER") {
        allowedObjects = <RackConstructionContainer/>;
    }

    return (
        <div>
            <RoomNameContainer/>
            <RoomTypeContainer/>
            {allowedObjects}
            <DeleteRoomContainer/>
        </div>
    );
};

export default RoomSidebarComponent;
