import React from 'react'
import LoadBarContainer from '../../../../../containers/app/sidebars/elements/LoadBarContainer'
import LoadChartContainer from '../../../../../containers/app/sidebars/elements/LoadChartContainer'
import BackToBuildingContainer from '../../../../../containers/app/sidebars/topology/room/BackToBuildingContainer'
import DeleteRoomContainer from '../../../../../containers/app/sidebars/topology/room/DeleteRoomContainer'
import EditRoomContainer from '../../../../../containers/app/sidebars/topology/room/EditRoomContainer'
import RackConstructionContainer from '../../../../../containers/app/sidebars/topology/room/RackConstructionContainer'
import RoomNameContainer from '../../../../../containers/app/sidebars/topology/room/RoomNameContainer'

const RoomSidebarComponent = ({ roomId, inSimulation }) => {
    return (
        <div>
            <RoomNameContainer/>
            <BackToBuildingContainer/>
            {inSimulation ? (
                <div>
                    <LoadBarContainer objectType="room" objectId={roomId}/>
                    <LoadChartContainer objectType="room" objectId={roomId}/>
                </div>
            ) : (
                <div>
                    <RackConstructionContainer/>
                    <EditRoomContainer/>
                    <DeleteRoomContainer/>
                </div>
            )}
        </div>
    )
}

export default RoomSidebarComponent
