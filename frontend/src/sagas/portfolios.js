import { call, put, select } from 'redux-saga/effects'
import { addPropToStoreObject, addToStore } from '../actions/objects'
import { addPortfolio, deletePortfolio, getPortfolio, updatePortfolio } from '../api/routes/portfolios'
import { getProject } from '../api/routes/projects'
import { fetchAndStoreAllSchedulers, fetchAndStoreAllTraces } from './objects'
import { fetchAndStoreAllTopologiesOfProject } from './topology'
import { getScenario } from '../api/routes/scenarios'

export function* onOpenPortfolioSucceeded(action) {
    try {
        const project = yield call(getProject, action.projectId)
        yield put(addToStore('project', project))
        yield fetchAndStoreAllTopologiesOfProject(project._id)
        yield fetchPortfoliosOfProject()
        yield fetchAndStoreAllSchedulers()
        yield fetchAndStoreAllTraces()

        // TODO Fetch portfolio-specific metrics
    } catch (error) {
        console.error(error)
    }
}

export function* fetchPortfoliosOfProject() {
    try {
        const currentProjectId = yield select((state) => state.currentProjectId)
        const currentProject = yield select((state) => state.objects.project[currentProjectId])

        yield fetchAndStoreAllSchedulers()
        yield fetchAndStoreAllTraces()

        for (let i in currentProject.portfolioIds) {
            yield fetchPortfolioWithScenarios(currentProject.portfolioIds[i])
        }
    } catch (error) {
        console.error(error)
    }
}

export function* fetchPortfolioWithScenarios(portfolioId) {
    try {
        const portfolio = yield call(getPortfolio, portfolioId)
        yield put(addToStore('portfolio', portfolio))

        for (let i in portfolio.scenarioIds) {
            const scenario = yield call(getScenario, portfolio.scenarioIds[i])
            yield put(addToStore('scenario', scenario))
        }
        return portfolio
    } catch (error) {
        console.error(error)
    }
}

export function* onAddPortfolio(action) {
    try {
        const currentProjectId = yield select((state) => state.currentProjectId)

        const portfolio = yield call(
            addPortfolio,
            currentProjectId,
            Object.assign({}, action.portfolio, {
                projectId: currentProjectId,
                scenarioIds: [],
            }),
        )
        yield put(addToStore('portfolio', portfolio))

        const portfolioIds = yield select((state) => state.objects.project[currentProjectId].portfolioIds)
        yield put(
            addPropToStoreObject('project', currentProjectId, {
                portfolioIds: portfolioIds.concat([portfolio._id]),
            }),
        )
    } catch (error) {
        console.error(error)
    }
}

export function* onUpdatePortfolio(action) {
    try {
        const portfolio = yield call(
            updatePortfolio,
            action.portfolio._id,
            action.portfolio,
        )
        yield put(addToStore('portfolio', portfolio))
    } catch (error) {
        console.error(error)
    }
}

export function* onDeletePortfolio(action) {
    try {
        yield call(deletePortfolio, action.id)

        const currentProjectId = yield select((state) => state.currentProjectId)
        const portfolioIds = yield select((state) => state.objects.project[currentProjectId].portfolioIds)

        yield put(
            addPropToStoreObject('project', currentProjectId, {
                portfolioIds: portfolioIds.filter((id) => id !== action.id),
            }),
        )
    } catch (error) {
        console.error(error)
    }
}
