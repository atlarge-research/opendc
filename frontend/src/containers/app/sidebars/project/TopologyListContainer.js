import { connect } from 'react-redux'
import TopologyListComponent from '../../../../components/app/sidebars/project/TopologyListComponent'
import { setCurrentTopology } from '../../../../actions/topology/building'
import { openNewTopologyModal } from '../../../../actions/modals/topology'
import { withRouter } from 'react-router-dom'
import { getState } from '../../../../util/state-utils'
import { deleteScenario } from '../../../../actions/scenarios'

const mapStateToProps = (state) => {
    let topologies = state.objects.project[state.currentProjectId]
        ? state.objects.project[state.currentProjectId].topologyIds.map((t) => state.objects.topology[t])
        : []
    if (topologies.filter((t) => !t).length > 0) {
        topologies = []
    }

    return {
        currentTopologyId: state.currentTopologyId,
        topologies,
    }
}

const mapDispatchToProps = (dispatch, ownProps) => {
    return {
        onChooseTopology: async (id) => {
            dispatch(setCurrentTopology(id))
            const state = await getState(dispatch)
            ownProps.history.push(`/projects/${state.currentProjectId}`)
        },
        onNewTopology: () => {
            dispatch(openNewTopologyModal())
        },
        onDeleteTopology: async (id) => {
            if (id) {
                const state = await getState(dispatch)
                dispatch(deleteScenario(id))
                dispatch(setCurrentTopology(state.objects.project[state.currentProjectId].topologyIds[0]))
                ownProps.history.push(`/projects/${state.currentProjectId}`)
            }
        },
    }
}

const TopologyListContainer = withRouter(connect(mapStateToProps, mapDispatchToProps)(TopologyListComponent))

export default TopologyListContainer
