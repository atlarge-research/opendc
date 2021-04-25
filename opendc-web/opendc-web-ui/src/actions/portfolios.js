export const ADD_PORTFOLIO = 'ADD_PORTFOLIO'
export const UPDATE_PORTFOLIO = 'UPDATE_PORTFOLIO'
export const DELETE_PORTFOLIO = 'DELETE_PORTFOLIO'
export const OPEN_PORTFOLIO_SUCCEEDED = 'OPEN_PORTFOLIO_SUCCEEDED'
export const SET_CURRENT_PORTFOLIO = 'SET_CURRENT_PORTFOLIO'

export function addPortfolio(portfolio) {
    return {
        type: ADD_PORTFOLIO,
        portfolio,
    }
}

export function updatePortfolio(portfolio) {
    return {
        type: UPDATE_PORTFOLIO,
        portfolio,
    }
}

export function deletePortfolio(id) {
    return {
        type: DELETE_PORTFOLIO,
        id,
    }
}

export function openPortfolioSucceeded(projectId, portfolioId) {
    return {
        type: OPEN_PORTFOLIO_SUCCEEDED,
        projectId,
        portfolioId,
    }
}

export function setCurrentPortfolio(portfolioId) {
    return {
        type: SET_CURRENT_PORTFOLIO,
        portfolioId,
    }
}
