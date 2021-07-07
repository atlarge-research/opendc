import { call, put, select, delay, getContext } from 'redux-saga/effects'
import { addPropToStoreObject, addToStore } from '../actions/objects'
import { addPortfolio, deletePortfolio, getPortfolio, updatePortfolio } from '../../api/portfolios'
import { getProject } from '../../api/projects'
import { fetchAndStoreAllSchedulers, fetchAndStoreAllTraces } from './objects'
import { fetchAndStoreAllTopologiesOfProject } from './topology'
import { getScenario } from '../../api/scenarios'

export function* onOpenPortfolioSucceeded(action) {
    try {
        const auth = yield getContext('auth')
        const project = yield call(getProject, auth, action.projectId)
        yield put(addToStore('project', project))
        yield fetchAndStoreAllTopologiesOfProject(project._id)
        yield fetchPortfoliosOfProject(project)
        yield fetchAndStoreAllSchedulers()
        yield fetchAndStoreAllTraces()

        yield watchForPortfolioResults(action.portfolioId)
    } catch (error) {
        console.error(error)
    }
}

export function* watchForPortfolioResults(portfolioId) {
    try {
        let unfinishedScenarios = yield getCurrentUnfinishedScenarios(portfolioId)

        while (unfinishedScenarios.length > 0) {
            yield delay(3000)
            yield fetchPortfolioWithScenarios(portfolioId)
            unfinishedScenarios = yield getCurrentUnfinishedScenarios(portfolioId)
        }
    } catch (error) {
        console.error(error)
    }
}

export function* getCurrentUnfinishedScenarios(portfolioId) {
    try {
        if (!portfolioId) {
            return []
        }

        const scenarioIds = yield select((state) => state.objects.portfolio[portfolioId].scenarioIds)
        const scenarioObjects = yield select((state) => state.objects.scenario)
        const scenarios = scenarioIds.map((s) => scenarioObjects[s])
        return scenarios.filter((s) => !s || s.simulation.state === 'QUEUED' || s.simulation.state === 'RUNNING')
    } catch (error) {
        console.error(error)
    }
}

export function* fetchPortfoliosOfProject(project) {
    try {
        yield fetchAndStoreAllSchedulers()
        yield fetchAndStoreAllTraces()

        for (const i in project.portfolioIds) {
            yield fetchPortfolioWithScenarios(project.portfolioIds[i])
        }
    } catch (error) {
        console.error(error)
    }
}

export function* fetchPortfolioWithScenarios(portfolioId) {
    try {
        const auth = yield getContext('auth')
        const portfolio = yield call(getPortfolio, auth, portfolioId)
        yield put(addToStore('portfolio', portfolio))

        for (let i in portfolio.scenarioIds) {
            const scenario = yield call(getScenario, auth, portfolio.scenarioIds[i])
            yield put(addToStore('scenario', scenario))
        }
        return portfolio
    } catch (error) {
        console.error(error)
    }
}

export function* onAddPortfolio(action) {
    try {
        const { projectId } = action
        const auth = yield getContext('auth')
        const portfolio = yield call(
            addPortfolio,
            auth,
            projectId,
            Object.assign({}, action.portfolio, {
                projectId: projectId,
                scenarioIds: [],
            })
        )
        yield put(addToStore('portfolio', portfolio))

        const portfolioIds = yield select((state) => state.objects.project[projectId].portfolioIds)
        yield put(
            addPropToStoreObject('project', projectId, {
                portfolioIds: portfolioIds.concat([portfolio._id]),
            })
        )
    } catch (error) {
        console.error(error)
    }
}

export function* onUpdatePortfolio(action) {
    try {
        const auth = yield getContext('auth')
        const portfolio = yield call(updatePortfolio, auth, action.portfolio._id, action.portfolio)
        yield put(addToStore('portfolio', portfolio))
    } catch (error) {
        console.error(error)
    }
}

export function* onDeletePortfolio(action) {
    try {
        const auth = yield getContext('auth')
        const portfolio = yield select((state) => state.objects.portfolio[action.id])

        yield call(deletePortfolio, auth, action.id)

        const portfolioIds = yield select((state) => state.objects.project[portfolio.projectId].portfolioIds)

        yield put(
            addPropToStoreObject('project', portfolio.projectId, {
                portfolioIds: portfolioIds.filter((id) => id !== action.id),
            })
        )
    } catch (error) {
        console.error(error)
    }
}
