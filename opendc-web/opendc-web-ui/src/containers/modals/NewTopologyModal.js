import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import NewTopologyModalComponent from '../../components/modals/custom-components/NewTopologyModalComponent'
import { closeNewTopologyModal } from '../../actions/modals/topology'
import { addTopology } from '../../actions/topologies'

const NewTopologyModal = () => {
    const show = useSelector((state) => state.modals.changeTopologyModalVisible)
    const topologies = useSelector((state) => {
        let topologies = state.objects.project[state.currentProjectId]
            ? state.objects.project[state.currentProjectId].topologyIds.map((t) => state.objects.topology[t])
            : []
        if (topologies.filter((t) => !t).length > 0) {
            topologies = []
        }

        return topologies
    })

    const dispatch = useDispatch()
    const onCreateTopology = (name) => {
        if (name) {
            dispatch(addTopology(name, undefined))
        }
        dispatch(closeNewTopologyModal())
    }
    const onDuplicateTopology = (name, id) => {
        if (name) {
            dispatch(addTopology(name, id))
        }
        dispatch(closeNewTopologyModal())
    }
    const onCancel = () => {
        dispatch(closeNewTopologyModal())
    }

    return (
        <NewTopologyModalComponent
            show={show}
            topologies={topologies}
            onCreateTopology={onCreateTopology}
            onDuplicateTopology={onDuplicateTopology}
            onCancel={onCancel}
        />
    )
}

export default NewTopologyModal
