import React from "react";
import {Group} from "react-konva";
import Shapes from "../../../shapes/index";
import RoomGroup from "./RoomGroup";

const DatacenterGroup = ({datacenter}) => {
    if (!datacenter) {
        return <Group/>;
    }
    return (
        <Group>
            {datacenter.rooms.map(room => (
                <RoomGroup key={room.id} room={room}/>
            ))}
        </Group>
    );
};

DatacenterGroup.propTypes = {
    datacenter: Shapes.Datacenter,
};

export default DatacenterGroup;
