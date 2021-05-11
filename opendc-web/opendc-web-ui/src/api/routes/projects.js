import { request } from '../index'

export function getProject(projectId) {
    return request(`projects/${projectId}`)
}

export function addProject(project) {
    return request('projects', 'POST', { project })
}

export function updateProject(project) {
    return request(`projects/${project._id}`, 'PUT', { project })
}

export function deleteProject(projectId) {
    return request(`projects/${projectId}`, 'DELETE')
}
