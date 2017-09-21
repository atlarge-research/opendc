import React from "react";
import {MAP_MAX_SCALE, MAP_MIN_SCALE, MAP_SCALE_PER_EVENT} from "../MapConstants";

const ZoomControlComponent = ({mapScale, setMapScale}) => {
    const zoom = (out) => {
        const newScale = out ? mapScale / MAP_SCALE_PER_EVENT : mapScale * MAP_SCALE_PER_EVENT;
        const boundedScale = Math.min(Math.max(MAP_MIN_SCALE, newScale), MAP_MAX_SCALE);
        setMapScale(boundedScale);
    };

    return (
        <span>
            <button
                className="btn btn-default btn-circle btn-sm mr-1"
                title="Zoom in"
                onClick={() => zoom(false)}
            >
                <span className="fa fa-plus"/>
            </button>
            <button
                className="btn btn-default btn-circle btn-sm mr-1"
                title="Zoom out"
                onClick={() => zoom(true)}
            >
                <span className="fa fa-minus"/>
            </button>
        </span>
    );
};

export default ZoomControlComponent;
