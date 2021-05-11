import { request } from '../index'

export function getUserByEmail(email) {
    return request(`users` + new URLSearchParams({ email }))
}

export function addUser(user) {
    return request('users', 'POST', { user })
}

export function getUser(userId) {
    return request(`users/${userId}`)
}

export function deleteUser(userId) {
    return request(`users/${userId}`, 'DELETE')
}
