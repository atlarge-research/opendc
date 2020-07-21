export const OPEN_NEW_PREFAB_MODAL = 'OPEN_NEW_PREFAB_MODAL'
export const CLOSE_NEW_PREFAB_MODAL = 'CLOSE_PREFAB_MODAL'

export function openNewPrefabModal() {
    return {
        type: OPEN_NEW_PREFAB_MODAL,
    }
}

export function closeNewPrefabModal() {
    return {
        type: CLOSE_NEW_PREFAB_MODAL,
    }
}
