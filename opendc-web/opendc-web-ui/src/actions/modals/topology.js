export const OPEN_NEW_TOPOLOGY_MODAL = 'OPEN_NEW_TOPOLOGY_MODAL'
export const CLOSE_NEW_TOPOLOGY_MODAL = 'CLOSE_NEW_TOPOLOGY_MODAL'
export const OPEN_EDIT_ROOM_NAME_MODAL = 'OPEN_EDIT_ROOM_NAME_MODAL'
export const CLOSE_EDIT_ROOM_NAME_MODAL = 'CLOSE_EDIT_ROOM_NAME_MODAL'
export const OPEN_DELETE_ROOM_MODAL = 'OPEN_DELETE_ROOM_MODAL'
export const CLOSE_DELETE_ROOM_MODAL = 'CLOSE_DELETE_ROOM_MODAL'
export const OPEN_EDIT_RACK_NAME_MODAL = 'OPEN_EDIT_RACK_NAME_MODAL'
export const CLOSE_EDIT_RACK_NAME_MODAL = 'CLOSE_EDIT_RACK_NAME_MODAL'
export const OPEN_DELETE_RACK_MODAL = 'OPEN_DELETE_RACK_MODAL'
export const CLOSE_DELETE_RACK_MODAL = 'CLOSE_DELETE_RACK_MODAL'
export const OPEN_DELETE_MACHINE_MODAL = 'OPEN_DELETE_MACHINE_MODAL'
export const CLOSE_DELETE_MACHINE_MODAL = 'CLOSE_DELETE_MACHINE_MODAL'

export function openNewTopologyModal() {
    return {
        type: OPEN_NEW_TOPOLOGY_MODAL,
    }
}

export function closeNewTopologyModal() {
    return {
        type: CLOSE_NEW_TOPOLOGY_MODAL,
    }
}

export function openEditRoomNameModal() {
    return {
        type: OPEN_EDIT_ROOM_NAME_MODAL,
    }
}

export function closeEditRoomNameModal() {
    return {
        type: CLOSE_EDIT_ROOM_NAME_MODAL,
    }
}

export function openDeleteRoomModal() {
    return {
        type: OPEN_DELETE_ROOM_MODAL,
    }
}

export function closeDeleteRoomModal() {
    return {
        type: CLOSE_DELETE_ROOM_MODAL,
    }
}

export function openEditRackNameModal() {
    return {
        type: OPEN_EDIT_RACK_NAME_MODAL,
    }
}

export function closeEditRackNameModal() {
    return {
        type: CLOSE_EDIT_RACK_NAME_MODAL,
    }
}

export function openDeleteRackModal() {
    return {
        type: OPEN_DELETE_RACK_MODAL,
    }
}

export function closeDeleteRackModal() {
    return {
        type: CLOSE_DELETE_RACK_MODAL,
    }
}

export function openDeleteMachineModal() {
    return {
        type: OPEN_DELETE_MACHINE_MODAL,
    }
}

export function closeDeleteMachineModal() {
    return {
        type: CLOSE_DELETE_MACHINE_MODAL,
    }
}
