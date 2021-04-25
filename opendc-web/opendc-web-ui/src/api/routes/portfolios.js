import { deleteById, getById } from './util'
import { sendRequest } from '../index'

export function addPortfolio(projectId, portfolio) {
    return sendRequest({
        path: '/projects/{projectId}/portfolios',
        method: 'POST',
        parameters: {
            body: {
                portfolio,
            },
            path: {
                projectId,
            },
            query: {},
        },
    })
}

export function getPortfolio(portfolioId) {
    return getById('/portfolios/{portfolioId}', { portfolioId })
}

export function updatePortfolio(portfolioId, portfolio) {
    return sendRequest({
        path: '/portfolios/{projectId}',
        method: 'POST',
        parameters: {
            body: {
                portfolio,
            },
            path: {
                portfolioId,
            },
            query: {},
        },
    })
}

export function deletePortfolio(portfolioId) {
    return deleteById('/portfolios/{portfolioId}', { portfolioId })
}
