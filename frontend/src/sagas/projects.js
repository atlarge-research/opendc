import { call, put } from 'redux-saga/effects'
import { addToStore } from '../actions/objects'
import { addProjectSucceeded, deleteProjectSucceeded } from '../actions/projects'
import { addProject, deleteProject, getProject } from '../api/routes/projects'
import { fetchAndStoreAllTopologiesOfProject } from './topology'

export function* onOpenProjectSucceeded(action) {
    try {
        const project = yield call(getProject, action.id)
        yield put(addToStore('project', project))

        yield fetchAndStoreAllTopologiesOfProject(action.id)
    } catch (error) {
        console.error(error)
    }
}

export function* onProjectAdd(action) {
    try {
        const project = yield call(addProject, { name: action.name })
        yield put(addToStore('project', project))

        const authorization = {
            projectId: project._id,
            userId: action.userId,
            authorizationLevel: 'OWN',
            project,
        }
        yield put(addToStore('authorization', authorization))
        yield put(addProjectSucceeded([authorization.userId, authorization.projectId]))
    } catch (error) {
        console.error(error)
    }
}

export function* onProjectDelete(action) {
    try {
        yield call(deleteProject, action.id)
        yield put(deleteProjectSucceeded(action.id))
    } catch (error) {
        console.error(error)
    }
}
