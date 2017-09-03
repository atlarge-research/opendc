import React from "react";
import RoomNameContainer from "../../../../containers/sidebars/topology/room/RoomNameContainer";
import RoomTypeContainer from "../../../../containers/sidebars/topology/room/RoomTypeContainer";

const RoomSidebarComponent = ({roomType}) => {
    let allowedObjects;
    if (roomType === "SERVER") {
        allowedObjects = "test";
    }

    return (
        <div>
            <RoomNameContainer/>
            <RoomTypeContainer/>
            {allowedObjects}
        </div>
    );
};

export default RoomSidebarComponent;
