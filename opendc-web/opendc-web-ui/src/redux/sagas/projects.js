import { call, put, getContext } from 'redux-saga/effects'
import { addToStore } from '../actions/objects'
import { addProjectSucceeded, deleteProjectSucceeded, fetchProjectsSucceeded } from '../actions/projects'
import { addProject, deleteProject, getProject, getProjects } from '../../api/projects'
import { fetchAndStoreAllTopologiesOfProject } from './topology'
import { fetchAndStoreAllSchedulers, fetchAndStoreAllTraces } from './objects'
import { fetchPortfoliosOfProject } from './portfolios'

export function* onOpenProjectSucceeded(action) {
    try {
        const auth = yield getContext('auth')
        const project = yield call(getProject, auth, action.id)
        yield put(addToStore('project', project))

        yield fetchAndStoreAllTopologiesOfProject(action.id, true)
        yield fetchPortfoliosOfProject()
        yield fetchAndStoreAllSchedulers()
        yield fetchAndStoreAllTraces()
    } catch (error) {
        console.error(error)
    }
}

export function* onProjectAdd(action) {
    try {
        const auth = yield getContext('auth')
        const project = yield call(addProject, auth, { name: action.name })
        yield put(addToStore('project', project))
        yield put(addProjectSucceeded(project))
    } catch (error) {
        console.error(error)
    }
}

export function* onProjectDelete(action) {
    try {
        const auth = yield getContext('auth')
        yield call(deleteProject, auth, action.id)
        yield put(deleteProjectSucceeded(action.id))
    } catch (error) {
        console.error(error)
    }
}

export function* onFetchProjects(action) {
    try {
        const auth = yield getContext('auth')
        const projects = yield call(getProjects, auth)
        yield put(fetchProjectsSucceeded(projects))
    } catch (error) {
        console.error(error)
    }
}
