import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import NewScenarioModalComponent from '../../components/modals/custom-components/NewScenarioModalComponent'
import { addScenario } from '../../actions/scenarios'
import { closeNewScenarioModal } from '../../actions/modals/scenarios'

const NewScenarioModal = (props) => {
    const topologies = useSelector(({ currentProjectId, objects }) => {
        console.log(currentProjectId, objects)

        if (currentProjectId === '-1' || !objects.project[currentProjectId]) {
            return []
        }

        const topologies = objects.project[currentProjectId].topologyIds.map((t) => objects.topology[t])

        if (topologies.filter((t) => !t).length > 0) {
            return []
        }

        return topologies
    })
    const state = useSelector((state) => {
        return {
            show: state.modals.newScenarioModalVisible,
            currentPortfolioId: state.currentPortfolioId,
            currentPortfolioScenarioIds:
                state.currentPortfolioId !== '-1' && state.objects.portfolio[state.currentPortfolioId]
                    ? state.objects.portfolio[state.currentPortfolioId].scenarioIds
                    : [],
            traces: Object.values(state.objects.trace),
            schedulers: Object.values(state.objects.scheduler),
        }
    })

    const dispatch = useDispatch()
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
        dispatch(closeNewScenarioModal())
    }

    return <NewScenarioModalComponent {...props} {...state} topologies={topologies} callback={callback} />
}

export default NewScenarioModal
