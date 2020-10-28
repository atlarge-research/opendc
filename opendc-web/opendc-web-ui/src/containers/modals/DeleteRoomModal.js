import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { closeDeleteRoomModal } from '../../actions/modals/topology'
import { deleteRoom } from '../../actions/topology/room'
import ConfirmationModal from '../../components/modals/ConfirmationModal'

const DeleteRoomModal = (props) => {
    const visible = useSelector((state) => state.modals.deleteRoomModalVisible)

    const dispatch = useDispatch()
    const callback = (isConfirmed) => {
        if (isConfirmed) {
            dispatch(deleteRoom())
        }
        dispatch(closeDeleteRoomModal())
    }
    return (
        <ConfirmationModal
            title="Delete this room"
            message="Are you sure you want to delete this room?"
            show={visible}
            callback={callback}
            {...props}
        />
    )
}

export default DeleteRoomModal
