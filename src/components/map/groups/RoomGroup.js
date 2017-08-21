import React from "react";
import {Group, Rect} from "react-konva";

const RoomGroup = () => (
    <Group>
        <Rect
            x={10}
            y={10}
            width={50}
            height={50}
            fill="green"
        />
    </Group>
);

export default RoomGroup;
