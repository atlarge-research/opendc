import React from 'react'
import { useDispatch } from 'react-redux'
import { openDeleteMachineModal } from '../../../../../actions/modals/topology'
import DeleteMachineComponent from '../../../../../components/app/sidebars/topology/machine/DeleteMachineComponent'

const DeleteMachineContainer = (props) => {
    const dispatch = useDispatch()
    return <DeleteMachineComponent {...props} onClick={() => dispatch(openDeleteMachineModal())} />
}

export default DeleteMachineContainer
