import PropTypes from 'prop-types'
import React, { useState } from 'react'
import ScenarioListComponent from '../../../../components/app/sidebars/project/ScenarioListComponent'
import NewScenarioModalComponent from '../../../../components/modals/custom-components/NewScenarioModalComponent'
import { useProjectTopologies } from '../../../../data/topology'
import { usePortfolio, useScenarios } from '../../../../data/project'
import { useSchedulers, useTraces } from '../../../../data/experiments'
import { useAuth } from '../../../../auth'
import { useMutation, useQueryClient } from 'react-query'
import { addScenario, deleteScenario } from '../../../../api/scenarios'

const ScenarioListContainer = ({ portfolioId }) => {
    const { data: portfolio } = usePortfolio(portfolioId)
    const scenarios = useScenarios(portfolio?.scenarioIds ?? [])
        .filter((res) => res.data)
        .map((res) => res.data)
    const topologies = useProjectTopologies()
    const traces = useTraces().data ?? []
    const schedulers = useSchedulers().data ?? []

    const auth = useAuth()
    const queryClient = useQueryClient()
    const addMutation = useMutation((data) => addScenario(auth, data), {
        onSuccess: async (result) => {
            await queryClient.invalidateQueries(['portfolios', portfolioId])
        },
    })
    const deleteMutation = useMutation((id) => deleteScenario(auth, id), {
        onSuccess: async (result) => {
            queryClient.setQueryData(['portfolios', portfolioId], (old) => ({
                ...old,
                scenarioIds: old.scenarioIds.filter((id) => id !== result._id),
            }))
            queryClient.removeQueries(['scenarios', result._id])
        },
    })

    const [isVisible, setVisible] = useState(false)

    const onNewScenario = (currentPortfolioId) => {
        setVisible(true)
    }
    const onDeleteScenario = (id) => {
        if (id) {
            deleteMutation.mutate(id)
        }
    }
    const callback = (name, portfolioId, trace, topology, operational) => {
        if (name) {
            addMutation.mutate({
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
