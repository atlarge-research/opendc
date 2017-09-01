export const OPEN_EDIT_ROOM_NAME_MODAL = "OPEN_EDIT_ROOM_NAME_MODAL";
export const CLOSE_EDIT_ROOM_NAME_MODAL = "CLOSE_EDIT_ROOM_NAME_MODAL";

export function openEditRoomNameModal() {
    return {
        type: OPEN_EDIT_ROOM_NAME_MODAL
    };
}

export function closeEditRoomNameModal() {
    return {
        type: CLOSE_EDIT_ROOM_NAME_MODAL
    };
}
