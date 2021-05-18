import { ADD_PROJECT_SUCCEEDED, DELETE_PROJECT_SUCCEEDED, FETCH_PROJECTS_SUCCEEDED } from '../actions/projects'

export function projects(state = [], action) {
    switch (action.type) {
        case FETCH_PROJECTS_SUCCEEDED:
            return action.projects
        case ADD_PROJECT_SUCCEEDED:
            return [...state, action.project]
        case DELETE_PROJECT_SUCCEEDED:
            return state.filter((project) => project._id !== action.id)
        default:
            return state
    }
}
