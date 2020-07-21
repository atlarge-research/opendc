import React from 'react'
import { connect } from 'react-redux'
import { closeDeleteRoomModal } from '../../actions/modals/topology'
import { deleteRoom } from '../../actions/topology/room'
import ConfirmationModal from '../../components/modals/ConfirmationModal'

const DeleteRoomModalComponent = ({ visible, callback }) => (
    <ConfirmationModal
        title="Delete this room"
        message="Are you sure you want to delete this room?"
        show={visible}
        callback={callback}
    />
)

const mapStateToProps = (state) => {
    return {
        visible: state.modals.deleteRoomModalVisible,
    }
}

const mapDispatchToProps = (dispatch) => {
    return {
        callback: (isConfirmed) => {
            if (isConfirmed) {
                dispatch(deleteRoom())
            }
            dispatch(closeDeleteRoomModal())
        },
    }
}

const DeleteRoomModal = connect(mapStateToProps, mapDispatchToProps)(DeleteRoomModalComponent)

export default DeleteRoomModal
