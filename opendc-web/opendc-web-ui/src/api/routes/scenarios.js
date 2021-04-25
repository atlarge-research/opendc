import { deleteById, getById } from './util'
import { sendRequest } from '../index'

export function addScenario(portfolioId, scenario) {
    return sendRequest({
        path: '/portfolios/{portfolioId}/scenarios',
        method: 'POST',
        parameters: {
            body: {
                scenario,
            },
            path: {
                portfolioId,
            },
            query: {},
        },
    })
}

export function getScenario(scenarioId) {
    return getById('/scenarios/{scenarioId}', { scenarioId })
}

export function updateScenario(scenarioId, scenario) {
    return sendRequest({
        path: '/scenarios/{projectId}',
        method: 'POST',
        parameters: {
            body: {
                scenario,
            },
            path: {
                scenarioId,
            },
            query: {},
        },
    })
}

export function deleteScenario(scenarioId) {
    return deleteById('/scenarios/{scenarioId}', { scenarioId })
}
