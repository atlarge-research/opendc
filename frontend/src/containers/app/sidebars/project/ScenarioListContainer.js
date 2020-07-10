import { connect } from 'react-redux'
import ScenarioListComponent from '../../../../components/app/sidebars/project/ScenarioListComponent'
import { openNewScenarioModal } from '../../../../actions/modals/scenarios'
import { deleteScenario, setCurrentScenario } from '../../../../actions/scenarios'
import { setCurrentPortfolio } from '../../../../actions/portfolios'

const mapStateToProps = (state, ownProps) => {
    let scenarios = state.objects.portfolio[ownProps.portfolioId] ? state.objects.portfolio[ownProps.portfolioId].scenarioIds.map(t => (
        state.objects.scenario[t]
    )) : []
    if (scenarios.filter(t => !t).length > 0) {
        scenarios = []
    }

    return {
        currentProjectId: state.currentProjectId,
        currentScenarioId: state.currentScenarioId,
        scenarios,
    }
}

const mapDispatchToProps = dispatch => {
    return {
        onNewScenario: (currentPortfolioId) => {
            dispatch(setCurrentPortfolio(currentPortfolioId))
            dispatch(openNewScenarioModal())
        },
        onChooseScenario: (portfolioId, scenarioId) => {
            dispatch(setCurrentScenario(portfolioId, scenarioId))
        },
        onDeleteScenario: (id) => {
            if (id) {
                dispatch(
                    deleteScenario(id),
                )
            }
        },
    }
}

const ScenarioListContainer = connect(mapStateToProps, mapDispatchToProps)(
    ScenarioListComponent,
)

export default ScenarioListContainer
