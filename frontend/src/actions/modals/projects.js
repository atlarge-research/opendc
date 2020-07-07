export const OPEN_NEW_PROJECT_MODAL = 'OPEN_NEW_PROJECT_MODAL'
export const CLOSE_NEW_PROJECT_MODAL = 'CLOSE_PROJECT_MODAL'

export function openNewProjectModal() {
    return {
        type: OPEN_NEW_PROJECT_MODAL,
    }
}

export function closeNewProjectModal() {
    return {
        type: CLOSE_NEW_PROJECT_MODAL,
    }
}
