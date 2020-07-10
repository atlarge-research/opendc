export const OPEN_NEW_PORTFOLIO_MODAL = 'OPEN_NEW_PORTFOLIO_MODAL'
export const CLOSE_NEW_PORTFOLIO_MODAL = 'CLOSE_PORTFOLIO_MODAL'

export function openNewPortfolioModal() {
    return {
        type: OPEN_NEW_PORTFOLIO_MODAL,
    }
}

export function closeNewPortfolioModal() {
    return {
        type: CLOSE_NEW_PORTFOLIO_MODAL,
    }
}
