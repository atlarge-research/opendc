import { connect } from 'react-redux'
import NewTopologyModalComponent from '../../components/modals/custom-components/NewTopologyModalComponent'
import { closeNewTopologyModal } from '../../actions/modals/topology'
import { addTopology } from '../../actions/topologies'

const mapStateToProps = (state) => {
    let topologies = state.objects.project[state.currentProjectId]
        ? state.objects.project[state.currentProjectId].topologyIds.map((t) => state.objects.topology[t])
        : []
    if (topologies.filter((t) => !t).length > 0) {
        topologies = []
    }

    return {
        show: state.modals.changeTopologyModalVisible,
        topologies,
    }
}

const mapDispatchToProps = (dispatch) => {
    return {
        onCreateTopology: (name) => {
            if (name) {
                dispatch(addTopology({ name, rooms: [] }))
            }
            dispatch(closeNewTopologyModal())
        },
        onDuplicateTopology: (name) => {
            if (name) {
                // TODO different handling here
                dispatch(addTopology({ name, rooms: [] }))
            }
            dispatch(closeNewTopologyModal())
        },
        onCancel: () => {
            dispatch(closeNewTopologyModal())
        },
    }
}

const NewTopologyModal = connect(mapStateToProps, mapDispatchToProps)(NewTopologyModalComponent)

export default NewTopologyModal
