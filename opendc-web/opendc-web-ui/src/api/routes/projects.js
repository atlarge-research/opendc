import { sendRequest } from '../index'
import { deleteById, getById } from './util'

export function getProject(projectId) {
    return getById('/projects/{projectId}', { projectId })
}

export function addProject(project) {
    return sendRequest({
        path: '/projects',
        method: 'POST',
        parameters: {
            body: {
                project,
            },
            path: {},
            query: {},
        },
    })
}

export function updateProject(project) {
    return sendRequest({
        path: '/projects/{projectId}',
        method: 'PUT',
        parameters: {
            body: {
                project,
            },
            path: {
                projectId: project._id,
            },
            query: {},
        },
    })
}

export function deleteProject(projectId) {
    return deleteById('/projects/{projectId}', { projectId })
}
