export const FETCH_PROJECTS = 'FETCH_PROJECTS'
export const FETCH_PROJECTS_SUCCEEDED = 'FETCH_PROJECTS_SUCCEEDED'
export const ADD_PROJECT = 'ADD_PROJECT'
export const ADD_PROJECT_SUCCEEDED = 'ADD_PROJECT_SUCCEEDED'
export const DELETE_PROJECT = 'DELETE_PROJECT'
export const DELETE_PROJECT_SUCCEEDED = 'DELETE_PROJECT_SUCCEEDED'
export const OPEN_PROJECT_SUCCEEDED = 'OPEN_PROJECT_SUCCEEDED'

export function fetchProjects() {
    return {
        type: FETCH_PROJECTS,
    }
}

export function fetchProjectsSucceeded(projects) {
    return {
        type: FETCH_PROJECTS_SUCCEEDED,
        projects,
    }
}

export function addProject(name) {
    return {
        type: ADD_PROJECT,
        name,
    }
}

export function addProjectSucceeded(project) {
    return {
        type: ADD_PROJECT_SUCCEEDED,
        project,
    }
}

export function deleteProject(id) {
    return {
        type: DELETE_PROJECT,
        id,
    }
}

export function deleteProjectSucceeded(id) {
    return {
        type: DELETE_PROJECT_SUCCEEDED,
        id,
    }
}

export function openProjectSucceeded(id) {
    return {
        type: OPEN_PROJECT_SUCCEEDED,
        id,
    }
}
