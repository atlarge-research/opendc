import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { closeDeleteRackModal } from '../../actions/modals/topology'
import { deleteRack } from '../../actions/topology/rack'
import ConfirmationModal from '../../components/modals/ConfirmationModal'

const DeleteRackModal = (props) => {
    const visible = useSelector((state) => state.modals.deleteRackModalVisible)
    const dispatch = useDispatch()
    const callback = (isConfirmed) => {
        if (isConfirmed) {
            dispatch(deleteRack())
        }
        dispatch(closeDeleteRackModal())
    }
    return (
        <ConfirmationModal
            title="Delete this rack"
            message="Are you sure you want to delete this rack?"
            show={visible}
            callback={callback}
            {...props}
        />
    )
}

export default DeleteRackModal
