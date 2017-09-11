export const SET_MAP_POSITION = "SET_MAP_POSITION";
export const SET_MAP_DIMENSIONS = "SET_MAP_DIMENSIONS";
export const SET_MAP_SCALE = "SET_MAP_SCALE";

export function setMapPosition(x, y) {
    return {
        type: SET_MAP_POSITION,
        x,
        y
    };
}

export function setMapDimensions(width, height) {
    return {
        type: SET_MAP_DIMENSIONS,
        width,
        height
    };
}

export function setMapScale(scale) {
    return {
        type: SET_MAP_SCALE,
        scale
    };
}
