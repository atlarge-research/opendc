import React from "react";
import {Group} from "react-konva";
import RoomGroup from "./RoomGroup";

const DatacenterGroup = ({datacenter}) => (
    <Group>
        {datacenter.rooms.map(room => (
            <RoomGroup room={room}/>
        ))}
    </Group>
);

DatacenterGroup.propTypes = {
    datacenter: Shapes.Datacenter,
};

export default DatacenterGroup;
