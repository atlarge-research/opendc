import { request } from '../index'

export function addPortfolio(projectId, portfolio) {
    return request(`projects/${projectId}/portfolios`, 'POST', { portfolio })
}

export function getPortfolio(portfolioId) {
    return request(`portfolios/${portfolioId}`)
}

export function updatePortfolio(portfolioId, portfolio) {
    return request(`portfolios/${portfolioId}`, 'PUT', { portfolio })
}

export function deletePortfolio(portfolioId) {
    return request(`portfolios/${portfolioId}`, 'DELETE')
}
