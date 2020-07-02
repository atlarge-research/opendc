import { connect } from 'react-redux'
import { addExperiment } from '../../actions/experiments'
import { closeNewExperimentModal } from '../../actions/modals/experiments'
import NewExperimentModalComponent from '../../components/modals/custom-components/NewExperimentModalComponent'

const mapStateToProps = state => {
    return {
        show: state.modals.newExperimentModalVisible,
        topologies: state.objects.simulation[state.currentSimulationId].topologyIds.map(t => (
            state.objects.topology[t]
        )),
        traces: Object.values(state.objects.trace),
        schedulers: Object.values(state.objects.scheduler),
    }
}

const mapDispatchToProps = dispatch => {
    return {
        callback: (name, topologyId, traceId, schedulerName) => {
            if (name) {
                dispatch(
                    addExperiment({
                        name,
                        topologyId,
                        traceId,
                        schedulerName,
                    }),
                )
            }
            dispatch(closeNewExperimentModal())
        },
    }
}

const NewExperimentModal = connect(mapStateToProps, mapDispatchToProps)(
    NewExperimentModalComponent,
)

export default NewExperimentModal
