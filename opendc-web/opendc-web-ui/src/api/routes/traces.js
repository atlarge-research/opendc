import { request } from '../index'

export function getAllTraces() {
    return request('traces')
}
