import PropTypes from 'prop-types'
import React, { useState } from 'react'
import { useDispatch } from 'react-redux'
import ScenarioListComponent from '../../../../components/app/sidebars/project/ScenarioListComponent'
import { addScenario, deleteScenario } from '../../../../redux/actions/scenarios'
import NewScenarioModalComponent from '../../../../components/modals/custom-components/NewScenarioModalComponent'
import { useProjectTopologies } from '../../../../data/topology'
import { useScenarios } from '../../../../data/project'
import { useSchedulers, useTraces } from '../../../../data/experiments'

const ScenarioListContainer = ({ portfolioId }) => {
    const scenarios = useScenarios(portfolioId)
    const topologies = useProjectTopologies()
    const traces = useTraces()
    const schedulers = useSchedulers()

    const dispatch = useDispatch()
    const [isVisible, setVisible] = useState(false)

    const onNewScenario = (currentPortfolioId) => {
        setVisible(true)
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
                scenarios={scenarios}
                onNewScenario={onNewScenario}
                onDeleteScenario={onDeleteScenario}
            />
            <NewScenarioModalComponent
                show={isVisible}
                currentPortfolioId={portfolioId}
                currentPortfolioScenarioIds={scenarios.map((s) => s._id)}
                traces={traces}
                schedulers={schedulers}
                topologies={topologies}
                callback={callback}
            />
        </>
    )
}

ScenarioListContainer.propTypes = {
    portfolioId: PropTypes.string,
}

export default ScenarioListContainer
