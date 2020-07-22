import { connect } from 'react-redux'
import NewScenarioModalComponent from '../../components/modals/custom-components/NewScenarioModalComponent'
import { addScenario } from '../../actions/scenarios'
import { closeNewScenarioModal } from '../../actions/modals/scenarios'

const mapStateToProps = (state) => {
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
        traces: Object.values(state.objects.trace),
        topologies,
        schedulers: Object.values(state.objects.scheduler),
    }
}

const mapDispatchToProps = (dispatch) => {
    return {
        callback: (name, portfolioId, trace, topology, operational) => {
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
        },
    }
}

const NewScenarioModal = connect(mapStateToProps, mapDispatchToProps)(NewScenarioModalComponent)

export default NewScenarioModal
