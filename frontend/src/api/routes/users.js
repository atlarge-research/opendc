import { sendRequest } from '../index'
import { deleteById } from './util'

export function getUserByEmail(email) {
    return sendRequest({
        path: '/users',
        method: 'GET',
        parameters: {
            body: {},
            path: {},
            query: {
                email,
            },
        },
    })
}

export function addUser(user) {
    return sendRequest({
        path: '/users',
        method: 'POST',
        parameters: {
            body: {
                user,
            },
            path: {},
            query: {},
        },
    })
}

export function getUser(userId) {
    return sendRequest({
        path: '/users/{userId}',
        method: 'GET',
        parameters: {
            body: {},
            path: {
                userId,
            },
            query: {},
        },
    })
}

export function deleteUser(userId) {
    return deleteById('/users/{userId}', { userId })
}
