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
                {datacenter.roomIds.map(roomId => (
                    <RoomContainer key={roomId} roomId={roomId}/>
                ))}
            </Group>
        );
    }

    return (
        <Group>
            {datacenter.roomIds
                .filter(roomId => roomId !== interactionLevel.roomId)
                .map(roomId => <RoomContainer key={roomId} roomId={roomId}/>)
            }
            {interactionLevel.mode === "ROOM" ? <GrayContainer/> : null}
            {datacenter.roomIds
                .filter(roomId => roomId === interactionLevel.roomId)
                .map(roomId => <RoomContainer key={roomId} roomId={roomId}/>)
            }
        </Group>
    );
};

DatacenterGroup.propTypes = {
    datacenter: Shapes.Datacenter,
    interactionLevel: Shapes.InteractionLevel,
};

export default DatacenterGroup;
