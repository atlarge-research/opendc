import { call, getContext } from 'redux-saga/effects'
import { fetchAndStoreAllTopologiesOfProject } from './topology'
import { fetchPortfoliosOfProject } from './portfolios'
import { fetchProject } from '../../api/projects'

export function* onOpenProjectSucceeded(action) {
    try {
        const auth = yield getContext('auth')
        const queryClient = yield getContext('queryClient')
        const project = yield call(() =>
            queryClient.fetchQuery(`projects/${action.id}`, () => fetchProject(auth, action.id))
        )

        yield fetchAndStoreAllTopologiesOfProject(action.id, true)
        yield fetchPortfoliosOfProject(project)
    } catch (error) {
        console.error(error)
    }
}
