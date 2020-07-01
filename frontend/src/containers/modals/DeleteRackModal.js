import React from 'react'
import { connect } from 'react-redux'
import { closeDeleteRackModal } from '../../actions/modals/topology'
import { deleteRack } from '../../actions/topology/rack'
import ConfirmationModal from '../../components/modals/ConfirmationModal'

const DeleteRackModalComponent = ({ visible, callback }) => (
    <ConfirmationModal
        title="Delete this rack"
        message="Are you sure you want to delete this rack?"
        show={visible}
        callback={callback}
    />
)

const mapStateToProps = state => {
    return {
        visible: state.modals.deleteRackModalVisible,
    }
}

const mapDispatchToProps = dispatch => {
    return {
        callback: isConfirmed => {
            if (isConfirmed) {
                dispatch(deleteRack())
            }
            dispatch(closeDeleteRackModal())
        },
    }
}

const DeleteRackModal = connect(mapStateToProps, mapDispatchToProps)(
    DeleteRackModalComponent,
)

export default DeleteRackModal
