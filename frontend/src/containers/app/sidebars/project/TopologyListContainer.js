import { connect } from 'react-redux'
import TopologyListComponent from '../../../../components/app/sidebars/project/TopologyListComponent'
import { setCurrentTopology } from '../../../../actions/topology/building'
import { openNewTopologyModal } from '../../../../actions/modals/topology'
import { deleteTopology } from '../../../../actions/topologies'

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
        },
        onNewTopology: () => {
            dispatch(openNewTopologyModal())
        },
        onDeleteTopology: (id) => {
            if (id) {
                dispatch(
                    deleteTopology(id),
                )
            }
        },
    }
}

const TopologyListContainer = connect(mapStateToProps, mapDispatchToProps)(
    TopologyListComponent,
)

export default TopologyListContainer
