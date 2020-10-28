import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { closeDeleteProfileModal } from '../../actions/modals/profile'
import { deleteCurrentUser } from '../../actions/users'
import ConfirmationModal from '../../components/modals/ConfirmationModal'

const DeleteProfileModal = () => {
    const visible = useSelector((state) => state.modals.deleteProfileModalVisible)

    const dispatch = useDispatch()
    const callback = (isConfirmed) => {
        if (isConfirmed) {
            dispatch(deleteCurrentUser())
        }
        dispatch(closeDeleteProfileModal())
    }
    return (
        <ConfirmationModal
            title="Delete my account"
            message="Are you sure you want to delete your OpenDC account?"
            show={visible}
            callback={callback}
        />
    )
}

export default DeleteProfileModal
