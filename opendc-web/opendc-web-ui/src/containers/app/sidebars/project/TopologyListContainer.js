import React, { useState } from 'react'
import { useDispatch } from 'react-redux'
import TopologyListComponent from '../../../../components/app/sidebars/project/TopologyListComponent'
import { setCurrentTopology } from '../../../../redux/actions/topology/building'
import { useRouter } from 'next/router'
import { addTopology } from '../../../../redux/actions/topologies'
import NewTopologyModalComponent from '../../../../components/modals/custom-components/NewTopologyModalComponent'
import { useActiveTopology, useTopologies } from '../../../../data/topology'
import { useProject } from '../../../../data/project'
import { useMutation } from 'react-query'

const TopologyListContainer = () => {
    const dispatch = useDispatch()
    const router = useRouter()
    const { project: currentProjectId } = router.query
    const { data: currentProject } = useProject(currentProjectId)
    const topologies = useTopologies(currentProject?.topologyIds ?? [])
        .filter((res) => res.data)
        .map((res) => ({ _id: res.data._id, name: res.data.name }))
    const currentTopologyId = useActiveTopology()?._id
    const [isVisible, setVisible] = useState(false)

    const { mutate: deleteTopology } = useMutation('deleteTopology')

    const onChooseTopology = async (id) => {
        dispatch(setCurrentTopology(id))
        await router.push(`/projects/${currentProjectId}/topologies/${id}`)
    }
    const onDeleteTopology = async (id) => {
        if (id) {
            deleteTopology(id)
            await router.push(`/projects/${currentProjectId}`)
        }
    }
    const onCreateTopology = (name) => {
        if (name) {
            dispatch(addTopology(currentProjectId, name, undefined))
        }
        setVisible(false)
    }
    const onDuplicateTopology = (name, id) => {
        if (name) {
            dispatch(addTopology(currentProjectId, name, id))
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
