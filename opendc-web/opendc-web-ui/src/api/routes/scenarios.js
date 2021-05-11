import { request } from '../index'

export function addScenario(portfolioId, scenario) {
    return request(`portfolios/${portfolioId}/scenarios`, 'POST', { scenario })
}

export function getScenario(scenarioId) {
    return request(`scenarios/${scenarioId}`)
}

export function updateScenario(scenarioId, scenario) {
    return request(`scenarios/${scenarioId}`, 'PUT', { scenario })
}

export function deleteScenario(scenarioId) {
    return request(`scenarios/${scenarioId}`, 'DELETE')
}
