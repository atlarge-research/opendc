import React, { useState } from 'react'
import { useDispatch } from 'react-redux'
import ScenarioListComponent from '../../../../components/app/sidebars/project/ScenarioListComponent'
import { addScenario, deleteScenario, setCurrentScenario } from '../../../../redux/actions/scenarios'
import { setCurrentPortfolio } from '../../../../redux/actions/portfolios'
import NewScenarioModalComponent from '../../../../components/modals/custom-components/NewScenarioModalComponent'
import { useProjectTopologies } from '../../../../data/topology'
import { useActiveScenario, useActiveProject, useScenarios } from '../../../../data/project'
import { useSchedulers, useTraces } from '../../../../data/experiments'

const ScenarioListContainer = ({ portfolioId }) => {
    const currentProjectId = useActiveProject()?._id
    const currentScenarioId = useActiveScenario()?._id
    const scenarios = useScenarios(portfolioId)
    const topologies = useProjectTopologies()
    const traces = useTraces()
    const schedulers = useSchedulers()

    const dispatch = useDispatch()
    const [isVisible, setVisible] = useState(false)

    const onNewScenario = (currentPortfolioId) => {
        dispatch(setCurrentPortfolio(currentPortfolioId))
        setVisible(true)
    }
    const onChooseScenario = (portfolioId, scenarioId) => {
        dispatch(setCurrentScenario(portfolioId, scenarioId))
    }
    const onDeleteScenario = (id) => {
        if (id) {
            dispatch(deleteScenario(id))
        }
    }
    const callback = (name, portfolioId, trace, topology, operational) => {
        if (name) {
            dispatch(
                addScenario({
                    portfolioId,
                    name,
                    trace,
                    topology,
                    operational,
                })
            )
        }

        setVisible(false)
    }

    return (
        <>
            <ScenarioListComponent
                portfolioId={portfolioId}
                currentProjectId={currentProjectId}
                currentScenarioId={currentScenarioId}
                scenarios={scenarios}
                onNewScenario={onNewScenario}
                onChooseScenario={onChooseScenario}
                onDeleteScenario={onDeleteScenario}
            />
            <NewScenarioModalComponent
                show={isVisible}
                currentPortfolioId={currentProjectId}
                currentPortfolioScenarioIds={scenarios.map((s) => s._id)}
                traces={traces}
                schedulers={schedulers}
                topologies={topologies}
                callback={callback}
            />
        </>
    )
}

export default ScenarioListContainer
