import React from "react";
import LoadBarContainer from "../../../../../containers/app/sidebars/elements/LoadBarContainer";
import LoadChartContainer from "../../../../../containers/app/sidebars/elements/LoadChartContainer";
import BackToBuildingContainer from "../../../../../containers/app/sidebars/topology/room/BackToBuildingContainer";
import DeleteRoomContainer from "../../../../../containers/app/sidebars/topology/room/DeleteRoomContainer";
import EditRoomContainer from "../../../../../containers/app/sidebars/topology/room/EditRoomContainer";
import RackConstructionContainer from "../../../../../containers/app/sidebars/topology/room/RackConstructionContainer";
import RoomNameContainer from "../../../../../containers/app/sidebars/topology/room/RoomNameContainer";
import RoomTypeContainer from "../../../../../containers/app/sidebars/topology/room/RoomTypeContainer";

const RoomSidebarComponent = ({ roomId, roomType, inSimulation }) => {
  let allowedObjects;
  if (!inSimulation && roomType === "SERVER") {
    allowedObjects = <RackConstructionContainer />;
  }

  return (
    <div>
      <RoomNameContainer />
      <RoomTypeContainer />
      <BackToBuildingContainer />
      {inSimulation ? (
        <div>
          <LoadBarContainer objectType="room" objectId={roomId} />
          <LoadChartContainer objectType="room" objectId={roomId} />
        </div>
      ) : (
        <div>
          {allowedObjects}
          <EditRoomContainer />
          <DeleteRoomContainer />
        </div>
      )}
    </div>
  );
};

export default RoomSidebarComponent;
