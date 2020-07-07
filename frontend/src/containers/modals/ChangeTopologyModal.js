import { connect } from 'react-redux'
import ChangeTopologyModalComponent from '../../components/modals/custom-components/ChangeTopologyModalComponent'
import { closeChangeTopologyModal } from '../../actions/modals/topology'
import { addTopology, deleteTopology } from '../../actions/topologies'
import { setCurrentTopology } from '../../actions/topology/building'

const mapStateToProps = state => {
    let topologies = state.objects.project[state.currentProjectId] ? state.objects.project[state.currentProjectId].topologyIds.map(t => (
        state.objects.topology[t]
    )) : []
    if (topologies.filter(t => !t).length > 0) {
        topologies = []
    }

    return {
        show: state.modals.changeTopologyModalVisible,
        currentTopologyId: state.currentTopologyId,
        topologies,
    }
}

const mapDispatchToProps = dispatch => {
    return {
        onChooseTopology: (id) => {
            dispatch(
                setCurrentTopology(id),
            )
            dispatch(closeChangeTopologyModal())
        },
        onCreateTopology: (name) => {
            if (name) {
                dispatch(
                    addTopology({ name, rooms: [] }),
                )
            }
            dispatch(closeChangeTopologyModal())
        },
        onDuplicateTopology: (name) => {
            if (name) {
                // TODO different handling here
                dispatch(
                    addTopology({ name, rooms: [] }),
                )
            }
            dispatch(closeChangeTopologyModal())
        },
        onDeleteTopology: (id) => {
            if (id) {
                dispatch(
                    deleteTopology(id),
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
