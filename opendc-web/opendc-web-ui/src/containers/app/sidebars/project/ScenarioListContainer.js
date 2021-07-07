import PropTypes from 'prop-types'
import React, { useState } from 'react'
import ScenarioListComponent from '../../../../components/app/sidebars/project/ScenarioListComponent'
import NewScenarioModalComponent from '../../../../components/modals/custom-components/NewScenarioModalComponent'
import { useTopologies } from '../../../../data/topology'
import { usePortfolio, useProject, useScenarios } from '../../../../data/project'
import { useSchedulers, useTraces } from '../../../../data/experiments'
import { useMutation } from 'react-query'

const ScenarioListContainer = ({ portfolioId }) => {
    const { data: portfolio } = usePortfolio(portfolioId)
    const { data: project } = useProject(portfolio?.projectId)
    const scenarios = useScenarios(portfolio?.scenarioIds ?? [])
        .filter((res) => res.data)
        .map((res) => res.data)
    const topologies = useTopologies(project?.topologyIds ?? [])
        .filter((res) => res.data)
        .map((res) => res.data)
    const traces = useTraces().data ?? []
    const schedulers = useSchedulers().data ?? []

    const { mutate: addScenario } = useMutation('addScenario')
    const { mutate: deleteScenario } = useMutation('deleteScenario')

    const [isVisible, setVisible] = useState(false)

    const onNewScenario = () => setVisible(true)
    const onDeleteScenario = (id) => id && deleteScenario(id)
    const callback = (name, portfolioId, trace, topology, operational) => {
        if (name) {
            addScenario({
                portfolioId,
                name,
                trace,
                topology,
                operational,
            })
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
