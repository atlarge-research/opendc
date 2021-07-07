import { fetchAndStoreAllTopologiesOfProject } from './topology'

export function* onOpenProjectSucceeded(action) {
    try {
        yield fetchAndStoreAllTopologiesOfProject(action.id, true)
    } catch (error) {
        console.error(error)
    }
}
