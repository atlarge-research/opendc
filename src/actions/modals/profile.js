export const OPEN_DELETE_PROFILE_MODAL = "OPEN_DELETE_PROFILE_MODAL";
export const CLOSE_DELETE_PROFILE_MODAL = "CLOSE_DELETE_PROFILE_MODAL";

export function openDeleteProfileModal() {
    return {
        type: OPEN_DELETE_PROFILE_MODAL
    };
}

export function closeDeleteProfileModal() {
    return {
        type: CLOSE_DELETE_PROFILE_MODAL
    };
}
