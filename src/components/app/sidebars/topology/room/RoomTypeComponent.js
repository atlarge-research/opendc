import React from "react";
import { ROOM_TYPE_TO_NAME_MAP } from "../../../../../util/room-types";

const RoomTypeComponent = ({ roomType }) => (
  <p className="lead">{ROOM_TYPE_TO_NAME_MAP[roomType]}</p>
);

export default RoomTypeComponent;
