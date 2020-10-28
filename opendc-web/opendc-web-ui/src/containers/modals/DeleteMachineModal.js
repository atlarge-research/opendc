import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { closeDeleteMachineModal } from '../../actions/modals/topology'
import { deleteMachine } from '../../actions/topology/machine'
import ConfirmationModal from '../../components/modals/ConfirmationModal'

const DeleteMachineModal = () => {
    const dispatch = useDispatch()
    const callback = (isConfirmed) => {
        if (isConfirmed) {
            dispatch(deleteMachine())
        }
        dispatch(closeDeleteMachineModal())
    }
    const visible = useSelector((state) => state.modals.deleteMachineModalVisible)
    return (
        <ConfirmationModal
            title="Delete this machine"
            message="Are you sure you want to delete this machine?"
            show={visible}
            callback={callback}
        />
    )
}

export default DeleteMachineModal
