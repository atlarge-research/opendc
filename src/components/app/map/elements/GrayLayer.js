import React from "react";
import {Rect} from "react-konva";
import {GRAYED_OUT_AREA_COLOR} from "../../../../util/colors";
import {MAP_SIZE_IN_PIXELS} from "../MapConstants";

const GrayLayer = ({onClick}) => (
    <Rect
        x={0}
        y={0}
        width={MAP_SIZE_IN_PIXELS}
        height={MAP_SIZE_IN_PIXELS}
        fill={GRAYED_OUT_AREA_COLOR}
        onClick={onClick}
    />
);

export default GrayLayer;
