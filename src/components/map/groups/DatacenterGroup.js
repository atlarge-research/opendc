import React from "react";
import {Group} from "react-konva";
import GrayContainer from "../../../containers/map/GrayContainer";
import RoomContainer from "../../../containers/map/RoomContainer";
import Shapes from "../../../shapes/index";

const DatacenterGroup = ({datacenter, interactionLevel}) => {
    if (!datacenter) {
        return <Group/>;
    }

    if (interactionLevel.mode === "ROOM") {
        return (
            <Group>
                {datacenter.rooms
                    .filter(room => room.id !== interactionLevel.roomId)
                    .map(room => <RoomContainer key={room.id} room={room}/>)
                }
                <GrayContainer/>
                {datacenter.rooms
                    .filter(room => room.id === interactionLevel.roomId)
                    .map(room => <RoomContainer key={room.id} room={room}/>)
                }
            </Group>
        );
    } else {
        return (
            <Group>
                {datacenter.rooms.map(room => (
                    <RoomContainer key={room.id} room={room}/>
                ))}
            </Group>
        );
    }
};

DatacenterGroup.propTypes = {
    datacenter: Shapes.Datacenter,
    interactionLevel: Shapes.InteractionLevel,
};

export default DatacenterGroup;
