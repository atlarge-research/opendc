export const GO_FROM_BUILDING_TO_ROOM = "GO_FROM_BUILDING_TO_ROOM";
export const GO_FROM_ROOM_TO_BUILDING = "GO_FROM_ROOM_TO_BUILDING";

export function goFromBuildingToRoom(roomId) {
    return {
        type: GO_FROM_BUILDING_TO_ROOM,
        roomId
    };
}

export function goFromRoomToBuilding() {
    return {
        type: GO_FROM_ROOM_TO_BUILDING
    };
}
