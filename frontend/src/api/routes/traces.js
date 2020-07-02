import { getAll } from './util'

export function getAllTraces() {
    return getAll('/traces')
}
