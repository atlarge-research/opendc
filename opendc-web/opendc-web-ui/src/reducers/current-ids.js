import { OPEN_PORTFOLIO_SUCCEEDED, SET_CURRENT_PORTFOLIO } from '../actions/portfolios'
import { OPEN_PROJECT_SUCCEEDED } from '../actions/projects'
import { SET_CURRENT_TOPOLOGY } from '../actions/topology/building'
import { OPEN_SCENARIO_SUCCEEDED, SET_CURRENT_SCENARIO } from '../actions/scenarios'

export function currentTopologyId(state = '-1', action) {
    switch (action.type) {
        case SET_CURRENT_TOPOLOGY:
            return action.topologyId
        default:
            return state
    }
}

export function currentProjectId(state = '-1', action) {
    switch (action.type) {
        case OPEN_PROJECT_SUCCEEDED:
            return action.id
        case OPEN_PORTFOLIO_SUCCEEDED:
        case OPEN_SCENARIO_SUCCEEDED:
            return action.projectId
        default:
            return state
    }
}

export function currentPortfolioId(state = '-1', action) {
    switch (action.type) {
        case OPEN_PORTFOLIO_SUCCEEDED:
        case SET_CURRENT_PORTFOLIO:
        case SET_CURRENT_SCENARIO:
            return action.portfolioId
        case OPEN_SCENARIO_SUCCEEDED:
            return action.portfolioId
        case OPEN_PROJECT_SUCCEEDED:
        case SET_CURRENT_TOPOLOGY:
            return '-1'
        default:
            return state
    }
}
export function currentScenarioId(state = '-1', action) {
    switch (action.type) {
        case OPEN_SCENARIO_SUCCEEDED:
        case SET_CURRENT_SCENARIO:
            return action.scenarioId
        case OPEN_PORTFOLIO_SUCCEEDED:
        case SET_CURRENT_TOPOLOGY:
        case OPEN_PROJECT_SUCCEEDED:
            return '-1'
        default:
            return state
    }
}
