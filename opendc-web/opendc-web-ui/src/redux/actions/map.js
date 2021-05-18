import {
    MAP_MAX_SCALE,
    MAP_MIN_SCALE,
    MAP_SCALE_PER_EVENT,
    MAP_SIZE_IN_PIXELS,
} from '../../components/app/map/MapConstants'

export const SET_MAP_POSITION = 'SET_MAP_POSITION'
export const SET_MAP_DIMENSIONS = 'SET_MAP_DIMENSIONS'
export const SET_MAP_SCALE = 'SET_MAP_SCALE'

export function setMapPosition(x, y) {
    return {
        type: SET_MAP_POSITION,
        x,
        y,
    }
}

export function setMapDimensions(width, height) {
    return {
        type: SET_MAP_DIMENSIONS,
        width,
        height,
    }
}

export function setMapScale(scale) {
    return {
        type: SET_MAP_SCALE,
        scale,
    }
}

export function zoomInOnCenter(zoomIn) {
    return (dispatch, getState) => {
        const state = getState()

        dispatch(zoomInOnPosition(zoomIn, state.map.dimensions.width / 2, state.map.dimensions.height / 2))
    }
}

export function zoomInOnPosition(zoomIn, x, y) {
    return (dispatch, getState) => {
        const state = getState()

        const centerPoint = {
            x: x / state.map.scale - state.map.position.x / state.map.scale,
            y: y / state.map.scale - state.map.position.y / state.map.scale,
        }
        const newScale = zoomIn ? state.map.scale * MAP_SCALE_PER_EVENT : state.map.scale / MAP_SCALE_PER_EVENT
        const boundedScale = Math.min(Math.max(MAP_MIN_SCALE, newScale), MAP_MAX_SCALE)

        const newX = -(centerPoint.x - x / boundedScale) * boundedScale
        const newY = -(centerPoint.y - y / boundedScale) * boundedScale

        dispatch(setMapPositionWithBoundsCheck(newX, newY))
        dispatch(setMapScale(boundedScale))
    }
}

export function setMapPositionWithBoundsCheck(x, y) {
    return (dispatch, getState) => {
        const state = getState()

        const scaledMapSize = MAP_SIZE_IN_PIXELS * state.map.scale

        const updatedX =
            x > 0
                ? 0
                : x < -scaledMapSize + state.map.dimensions.width
                ? -scaledMapSize + state.map.dimensions.width
                : x
        const updatedY =
            y > 0
                ? 0
                : y < -scaledMapSize + state.map.dimensions.height
                ? -scaledMapSize + state.map.dimensions.height
                : y

        dispatch(setMapPosition(updatedX, updatedY))
    }
}
