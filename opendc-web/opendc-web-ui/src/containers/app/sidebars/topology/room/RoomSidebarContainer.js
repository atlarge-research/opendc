import React from 'react'
import { useSelector } from 'react-redux'
import RoomSidebarComponent from '../../../../../components/app/sidebars/topology/room/RoomSidebarComponent'

const RoomSidebarContainer = (props) => {
    const roomId = useSelector((state) => state.interactionLevel.roomId)
    return <RoomSidebarComponent {...props} roomId={roomId} />
}

export default RoomSidebarContainer
