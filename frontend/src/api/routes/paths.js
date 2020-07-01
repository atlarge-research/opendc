import { sendRequest } from '../index'
import { getById } from './util'

export function getPath(pathId) {
    return getById('/paths/{pathId}', { pathId })
}

export function getBranchesOfPath(pathId) {
    return getById('/paths/{pathId}/branches', { pathId })
}

export function branchFromPath(pathId, section) {
    return sendRequest({
        path: '/paths/{pathId}/branches',
        method: 'POST',
        parameters: {
            body: {
                section,
            },
            path: {
                pathId,
            },
            query: {},
        },
    })
}

export function getSectionsOfPath(pathId) {
    return getById('/paths/{pathId}/sections', { pathId })
}
