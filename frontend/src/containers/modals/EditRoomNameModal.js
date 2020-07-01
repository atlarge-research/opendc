import React from 'react'
import { connect } from 'react-redux'
import { closeEditRoomNameModal } from '../../actions/modals/topology'
import { editRoomName } from '../../actions/topology/room'
import TextInputModal from '../../components/modals/TextInputModal'

const EditRoomNameModalComponent = ({ visible, previousName, callback }) => (
    <TextInputModal
        title="Edit room name"
        label="Room name"
        show={visible}
        initialValue={previousName}
        callback={callback}
    />
)

const mapStateToProps = state => {
    return {
        visible: state.modals.editRoomNameModalVisible,
        previousName:
            state.interactionLevel.mode === 'ROOM'
                ? state.objects.room[state.interactionLevel.roomId].name
                : '',
    }
}

const mapDispatchToProps = dispatch => {
    return {
        callback: name => {
            if (name) {
                dispatch(editRoomName(name))
            }
            dispatch(closeEditRoomNameModal())
        },
    }
}

const EditRoomNameModal = connect(mapStateToProps, mapDispatchToProps)(
    EditRoomNameModalComponent,
)

export default EditRoomNameModal
