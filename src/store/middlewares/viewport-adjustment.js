import {SET_MAP_DIMENSIONS, setMapPosition, setMapScale} from "../../actions/map";
import {
    MAP_MAX_SCALE,
    MAP_MIN_SCALE,
    SIDEBAR_WIDTH,
    TILE_SIZE_IN_PIXELS,
    VIEWPORT_PADDING
} from "../../components/map/MapConstants";
import {calculateRoomListBounds} from "../../util/tile-calculations";

export const viewportAdjustmentMiddleware = store => next => action => {
    const state = store.getState();
    if (action.type === SET_MAP_DIMENSIONS && state.currentDatacenterId !== -1) {
        const roomIds = state.objects.datacenter[state.currentDatacenterId].roomIds;
        const rooms = roomIds.map(id => Object.assign({}, state.objects.room[id]));
        rooms.forEach(room => room.tiles = room.tileIds.map(tileId => state.objects.tile[tileId]));

        const viewportParams = calculateParametersToZoomInOnRooms(rooms, action.width, action.height);
        store.dispatch(setMapPosition(viewportParams.newX, viewportParams.newY));
        store.dispatch(setMapScale(viewportParams.newScale));
    }

    next(action);
};

function calculateParametersToZoomInOnRooms(rooms, mapWidth, mapHeight) {
    const bounds = calculateRoomListBounds(rooms);
    const newScale = calculateNewScale(bounds, mapWidth, mapHeight);

    // Coordinates of the center of the room, relative to the global origin of the map
    const roomCenterCoordinates = {
        x: bounds.center.x * TILE_SIZE_IN_PIXELS * newScale,
        y: bounds.center.y * TILE_SIZE_IN_PIXELS * newScale
    };

    const newX = -roomCenterCoordinates.x + mapWidth / 2;
    const newY = -roomCenterCoordinates.y + mapHeight / 2;

    return {newScale, newX, newY};
}

function calculateNewScale(bounds, mapWidth, mapHeight) {
    const width = bounds.max.x - bounds.min.x;
    const height = bounds.max.y - bounds.min.y;

    const scaleX = (mapWidth - 2 * SIDEBAR_WIDTH) / (width * TILE_SIZE_IN_PIXELS + 2 * VIEWPORT_PADDING);
    const scaleY = mapHeight / (height * TILE_SIZE_IN_PIXELS + 2 * VIEWPORT_PADDING);
    const newScale = Math.min(scaleX, scaleY);

    return Math.min(Math.max(MAP_MIN_SCALE, newScale), MAP_MAX_SCALE);
}
