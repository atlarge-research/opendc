import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { closeEditRoomNameModal } from '../../actions/modals/topology'
import { editRoomName } from '../../actions/topology/room'
import TextInputModal from '../../components/modals/TextInputModal'

const EditRoomNameModal = (props) => {
    const visible = useSelector((state) => state.modals.editRoomNameModalVisible)
    const previousName = useSelector((state) =>
        state.interactionLevel.mode === 'ROOM' ? state.objects.room[state.interactionLevel.roomId].name : ''
    )

    const dispatch = useDispatch()
    const callback = (name) => {
        if (name) {
            dispatch(editRoomName(name))
        }
        dispatch(closeEditRoomNameModal())
    }
    return (
        <TextInputModal
            title="Edit room name"
            label="Room name"
            show={visible}
            initialValue={previousName}
            callback={callback}
            {...props}
        />
    )
}
export default EditRoomNameModal
