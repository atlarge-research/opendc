import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import TopologyListComponent from '../../../../components/app/sidebars/project/TopologyListComponent'
import { setCurrentTopology } from '../../../../actions/topology/building'
import { openNewTopologyModal } from '../../../../actions/modals/topology'
import { useHistory } from 'react-router-dom'
import { getState } from '../../../../util/state-utils'
import { deleteTopology } from '../../../../actions/topologies'

const TopologyListContainer = () => {
    const dispatch = useDispatch()
    const history = useHistory()

    const topologies = useSelector((state) => {
        let topologies = state.objects.project[state.currentProjectId]
            ? state.objects.project[state.currentProjectId].topologyIds.map((t) => state.objects.topology[t])
            : []
        if (topologies.filter((t) => !t).length > 0) {
            topologies = []
        }

        return topologies
    })
    const currentTopologyId = useSelector((state) => state.currentTopologyId)

    const onChooseTopology = async (id) => {
        dispatch(setCurrentTopology(id))
        const state = await getState(dispatch)
        history.push(`/projects/${state.currentProjectId}`)
    }
    const onNewTopology = () => {
        dispatch(openNewTopologyModal())
    }
    const onDeleteTopology = async (id) => {
        if (id) {
            const state = await getState(dispatch)
            dispatch(deleteTopology(id))
            dispatch(setCurrentTopology(state.objects.project[state.currentProjectId].topologyIds[0]))
            history.push(`/projects/${state.currentProjectId}`)
        }
    }

    return (
        <TopologyListComponent
            topologies={topologies}
            currentTopologyId={currentTopologyId}
            onChooseTopology={onChooseTopology}
            onNewTopology={onNewTopology}
            onDeleteTopology={onDeleteTopology}
        />
    )
}

export default TopologyListContainer
