import { request } from '../index'

export function getAllSchedulers() {
    return request('schedulers')
}
