import { connect } from 'react-redux'
import ChangeTopologyModalComponent from '../../components/modals/custom-components/ChangeTopologyModalComponent'
import { closeChangeTopologyModal } from '../../actions/modals/topology'
import { addTopology, deleteTopology } from '../../actions/topologies'

const mapStateToProps = state => {
    let topologies = state.objects.simulation[state.currentSimulationId] ? state.objects.simulation[state.currentSimulationId].topologyIds.map(t => (
        state.objects.topology[t]
    )) : []
    if (topologies.filter(t => !t).length > 0) {
        topologies = []
    }

    return {
        show: state.modals.newExperimentModalVisible,
        topologies,
    }
}

const mapDispatchToProps = dispatch => {
    return {
        onCreateTopology: (name) => {
            if (name) {
                dispatch(
                    addTopology({name})
                )
            }
            dispatch(closeChangeTopologyModal())
        },
        onDuplicateTopology: (name) => {
            if (name) {
                // TODO different handling here
                dispatch(
                    addTopology({name})
                )
            }
            dispatch(closeChangeTopologyModal())
        },
        onDeleteTopology: (id) => {
            if (id) {
                dispatch(
                    deleteTopology(id)
                )
            }
        },
        onCancel: () => {
            dispatch(closeChangeTopologyModal())
        },
    }
}

const ChangeTopologyModal = connect(mapStateToProps, mapDispatchToProps)(
    ChangeTopologyModalComponent,
)

export default ChangeTopologyModal
