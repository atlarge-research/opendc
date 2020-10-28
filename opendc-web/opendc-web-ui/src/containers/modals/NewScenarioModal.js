import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import NewScenarioModalComponent from '../../components/modals/custom-components/NewScenarioModalComponent'
import { addScenario } from '../../actions/scenarios'
import { closeNewScenarioModal } from '../../actions/modals/scenarios'

const NewScenarioModal = (props) => {
    const state = useSelector((state) => {
        let topologies =
            state.currentProjectId !== '-1'
                ? state.objects.project[state.currentProjectId].topologyIds.map((t) => state.objects.topology[t])
                : []
        if (topologies.filter((t) => !t).length > 0) {
            topologies = []
        }

        return {
            show: state.modals.newScenarioModalVisible,
            currentPortfolioId: state.currentPortfolioId,
            currentPortfolioScenarioIds:
                state.currentPortfolioId !== '-1' && state.objects.portfolio[state.currentPortfolioId]
                    ? state.objects.portfolio[state.currentPortfolioId].scenarioIds
                    : [],
            traces: Object.values(state.objects.trace),
            topologies,
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
    return <NewScenarioModalComponent {...props} {...state} callback={callback} />
}

export default NewScenarioModal
