import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { openEditRoomNameModal } from '../../../../../actions/modals/topology'
import RoomNameComponent from '../../../../../components/app/sidebars/topology/room/RoomNameComponent'

const RoomNameContainer = (props) => {
    const roomName = useSelector((state) => state.objects.room[state.interactionLevel.roomId].name)
    const dispatch = useDispatch()
    const onEdit = () => dispatch(openEditRoomNameModal())
    return <RoomNameComponent {...props} onEdit={onEdit} roomName={roomName} />
}

export default RoomNameContainer
