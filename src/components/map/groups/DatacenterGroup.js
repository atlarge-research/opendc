import React from "react";
import {Group} from "react-konva";
import GrayContainer from "../../../containers/map/GrayContainer";
import RoomContainer from "../../../containers/map/RoomContainer";
import Shapes from "../../../shapes/index";

const DatacenterGroup = ({datacenter, interactionLevel}) => {
    if (!datacenter) {
        return <Group/>;
    }

    if (interactionLevel.mode === "BUILDING") {
        return (
            <Group>
                {datacenter.rooms.map(room => (
                    <RoomContainer key={room.id} room={room}/>
                ))}
            </Group>
        );
    }

    return (
        <Group>
            {datacenter.rooms
                .filter(room => room.id !== interactionLevel.roomId)
                .map(room => <RoomContainer key={room.id} room={room}/>)
            }
            {interactionLevel.mode === "ROOM" ? <GrayContainer/> : null}
            {datacenter.rooms
                .filter(room => room.id === interactionLevel.roomId)
                .map(room => <RoomContainer key={room.id} room={room}/>)
            }
        </Group>
    );
};

DatacenterGroup.propTypes = {
    datacenter: Shapes.Datacenter,
    interactionLevel: Shapes.InteractionLevel,
};

export default DatacenterGroup;
