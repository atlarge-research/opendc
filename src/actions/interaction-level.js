export const GO_FROM_BUILDING_TO_ROOM = "GO_FROM_BUILDING_TO_ROOM";
export const GO_FROM_ROOM_TO_OBJECT = "GO_FROM_ROOM_TO_OBJECT";
export const GO_DOWN_ONE_INTERACTION_LEVEL = "GO_DOWN_ONE_INTERACTION_LEVEL";

export function goFromBuildingToRoom(roomId) {
    return (dispatch, getState) => {
        const {interactionLevel} = getState();
        if (interactionLevel.mode !== "BUILDING") {
            return;
        }

        dispatch({
            type: GO_FROM_BUILDING_TO_ROOM,
            roomId
        });
    };
}

export function goFromRoomToObject(tileId) {
    return (dispatch, getState) => {
        const {interactionLevel} = getState();
        if (interactionLevel.mode !== "ROOM") {
            return;
        }
        dispatch({
            type: GO_FROM_ROOM_TO_OBJECT,
            tileId
        });
    };
}

export function goDownOneInteractionLevel() {
    return {
        type: GO_DOWN_ONE_INTERACTION_LEVEL
    };
}
