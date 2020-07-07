import React from 'react'
import BackToBuildingContainer from '../../../../../containers/app/sidebars/topology/room/BackToBuildingContainer'
import DeleteRoomContainer from '../../../../../containers/app/sidebars/topology/room/DeleteRoomContainer'
import EditRoomContainer from '../../../../../containers/app/sidebars/topology/room/EditRoomContainer'
import RackConstructionContainer from '../../../../../containers/app/sidebars/topology/room/RackConstructionContainer'
import RoomNameContainer from '../../../../../containers/app/sidebars/topology/room/RoomNameContainer'

const RoomSidebarComponent = () => {
    return (
        <div>
            <RoomNameContainer/>
            <BackToBuildingContainer/>
            <RackConstructionContainer/>
            <EditRoomContainer/>
            <DeleteRoomContainer/>
        </div>
    )
}

export default RoomSidebarComponent
