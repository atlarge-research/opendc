import { combineReducers } from 'redux'
import { ADD_PROJECT_SUCCEEDED, DELETE_PROJECT_SUCCEEDED, SET_AUTH_VISIBILITY_FILTER } from '../actions/projects'
import { FETCH_AUTHORIZATIONS_OF_CURRENT_USER_SUCCEEDED } from '../actions/users'

export function authorizationsOfCurrentUser(state = [], action) {
    switch (action.type) {
        case FETCH_AUTHORIZATIONS_OF_CURRENT_USER_SUCCEEDED:
            return action.authorizationsOfCurrentUser
        case ADD_PROJECT_SUCCEEDED:
            return [...state, action.authorization]
        case DELETE_PROJECT_SUCCEEDED:
            return state.filter((authorization) => authorization[1] !== action.id)
        default:
            return state
    }
}

export function authVisibilityFilter(state = 'SHOW_ALL', action) {
    switch (action.type) {
        case SET_AUTH_VISIBILITY_FILTER:
            return action.filter
        default:
            return state
    }
}

export const projectList = combineReducers({
    authorizationsOfCurrentUser,
    authVisibilityFilter,
})
