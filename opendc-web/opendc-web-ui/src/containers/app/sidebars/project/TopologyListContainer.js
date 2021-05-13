import React, { useState } from 'react'
import { useDispatch } from 'react-redux'
import TopologyListComponent from '../../../../components/app/sidebars/project/TopologyListComponent'
import { setCurrentTopology } from '../../../../actions/topology/building'
import { useRouter } from 'next/router'
import { getState } from '../../../../util/state-utils'
import { addTopology, deleteTopology } from '../../../../actions/topologies'
import NewTopologyModalComponent from '../../../../components/modals/custom-components/NewTopologyModalComponent'
import { useActiveTopology, useProjectTopologies } from '../../../../store/hooks/topology'

const TopologyListContainer = () => {
    const dispatch = useDispatch()
    const router = useRouter()
    const topologies = useProjectTopologies()
    const currentTopologyId = useActiveTopology()?._id
    const [isVisible, setVisible] = useState(false)

    const onChooseTopology = async (id) => {
        dispatch(setCurrentTopology(id))
        const state = await getState(dispatch)
        router.push(`/projects/${state.currentProjectId}`)
    }
    const onDeleteTopology = async (id) => {
        if (id) {
            const state = await getState(dispatch)
            dispatch(deleteTopology(id))
            dispatch(setCurrentTopology(state.objects.project[state.currentProjectId].topologyIds[0]))
            router.push(`/projects/${state.currentProjectId}`)
        }
    }
    const onCreateTopology = (name) => {
        if (name) {
            dispatch(addTopology(name, undefined))
        }
        setVisible(false)
    }
    const onDuplicateTopology = (name, id) => {
        if (name) {
            dispatch(addTopology(name, id))
        }
        setVisible(false)
    }
    const onCancel = () => setVisible(false)

    return (
        <>
            <TopologyListComponent
                topologies={topologies}
                currentTopologyId={currentTopologyId}
                onChooseTopology={onChooseTopology}
                onNewTopology={() => setVisible(true)}
                onDeleteTopology={onDeleteTopology}
            />
            <NewTopologyModalComponent
                show={isVisible}
                topologies={topologies}
                onCreateTopology={onCreateTopology}
                onDuplicateTopology={onDuplicateTopology}
                onCancel={onCancel}
            />
        </>
    )
}

export default TopologyListContainer
