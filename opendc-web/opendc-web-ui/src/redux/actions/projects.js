export const OPEN_PROJECT_SUCCEEDED = 'OPEN_PROJECT_SUCCEEDED'

export function openProjectSucceeded(id) {
    return {
        type: OPEN_PROJECT_SUCCEEDED,
        id,
    }
}
