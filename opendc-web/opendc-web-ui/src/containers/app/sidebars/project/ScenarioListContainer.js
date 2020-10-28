import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import ScenarioListComponent from '../../../../components/app/sidebars/project/ScenarioListComponent'
import { openNewScenarioModal } from '../../../../actions/modals/scenarios'
import { deleteScenario, setCurrentScenario } from '../../../../actions/scenarios'
import { setCurrentPortfolio } from '../../../../actions/portfolios'

const ScenarioListContainer = ({ portfolioId, children }) => {
    const currentProjectId = useSelector((state) => state.currentProjectId)
    const currentScenarioId = useSelector((state) => state.currentScenarioId)
    const scenarios = useSelector((state) => {
        let scenarios = state.objects.portfolio[portfolioId]
            ? state.objects.portfolio[portfolioId].scenarioIds.map((t) => state.objects.scenario[t])
            : []
        if (scenarios.filter((t) => !t).length > 0) {
            scenarios = []
        }

        return scenarios
    })

    const dispatch = useDispatch()
    const onNewScenario = (currentPortfolioId) => {
        dispatch(setCurrentPortfolio(currentPortfolioId))
        dispatch(openNewScenarioModal())
    }
    const onChooseScenario = (portfolioId, scenarioId) => {
        dispatch(setCurrentScenario(portfolioId, scenarioId))
    }
    const onDeleteScenario = (id) => {
        if (id) {
            dispatch(deleteScenario(id))
        }
    }

    return (
        <ScenarioListComponent
            portfolioId={portfolioId}
            currentProjectId={currentProjectId}
            currentScenarioId={currentScenarioId}
            scenarios={scenarios}
            onNewScenario={onNewScenario}
            onChooseScenario={onChooseScenario}
            onDeleteScenario={onDeleteScenario}
        />
    )
}

export default ScenarioListContainer
