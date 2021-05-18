import React, { useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import NameComponent from '../../../../../components/app/sidebars/topology/NameComponent'
import TextInputModal from '../../../../../components/modals/TextInputModal'
import { editRoomName } from '../../../../../redux/actions/topology/room'

const RoomNameContainer = () => {
    const [isVisible, setVisible] = useState(false)
    const roomName = useSelector((state) => state.objects.room[state.interactionLevel.roomId].name)
    const dispatch = useDispatch()
    const callback = (name) => {
        if (name) {
            dispatch(editRoomName(name))
        }
        setVisible(false)
    }
    return (
        <>
            <NameComponent name={roomName} onEdit={() => setVisible(true)} />
            <TextInputModal
                title="Edit room name"
                label="Room name"
                show={isVisible}
                initialValue={roomName}
                callback={callback}
            />
        </>
    )
}

export default RoomNameContainer
