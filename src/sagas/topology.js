import {call, put, select} from "redux-saga/effects";
import {addPropToStoreObject, addToStore} from "../actions/objects";
import {
    addTileSucceeded,
    cancelNewRoomConstructionSucceeded,
    deleteTileSucceeded,
    editRoomNameSucceeded,
    fetchLatestDatacenterSucceeded,
    startNewRoomConstructionSucceeded
} from "../actions/topology";
import {addRoomToDatacenter} from "../api/routes/datacenters";
import {addTileToRoom, deleteRoom, updateRoom} from "../api/routes/rooms";
import {deleteTile} from "../api/routes/tiles";
import {
    fetchAndStoreCoolingItem,
    fetchAndStoreDatacenter,
    fetchAndStorePathsOfSimulation,
    fetchAndStorePSU,
    fetchAndStoreRackOnTile,
    fetchAndStoreRoomsOfDatacenter,
    fetchAndStoreSectionsOfPath,
    fetchAndStoreTilesOfRoom
} from "./objects";

export function* onFetchLatestDatacenter(action) {
    try {
        const paths = yield fetchAndStorePathsOfSimulation(action.currentSimulationId);
        const latestPath = paths[paths.length - 1];
        const sections = yield fetchAndStoreSectionsOfPath(latestPath.id);
        const latestSection = sections[sections.length - 1];
        yield fetchDatacenter(latestSection.datacenterId);
        yield put(fetchLatestDatacenterSucceeded(latestSection.datacenterId));
    } catch (error) {
        console.log(error);
    }
}

export function* fetchDatacenter(datacenterId) {
    try {
        yield fetchAndStoreDatacenter(datacenterId);
        const rooms = yield fetchAndStoreRoomsOfDatacenter(datacenterId);
        yield put(addPropToStoreObject("datacenter", datacenterId, {roomIds: rooms.map(room => room.id)}));

        for (let index in rooms) {
            yield fetchRoom(rooms[index].id);
        }
    } catch (error) {
        console.log(error);
    }
}

function* fetchRoom(roomId) {
    const tiles = yield fetchAndStoreTilesOfRoom(roomId);
    yield put(addPropToStoreObject("room", roomId, {tileIds: tiles.map(tile => tile.id)}));

    for (let index in tiles) {
        yield fetchTile(tiles[index]);
    }
}

function* fetchTile(tile) {
    if (!tile.objectType) {
        return;
    }

    switch (tile.objectType) {
        case "RACK":
            const rack = yield fetchAndStoreRackOnTile(tile.objectId, tile.id);
            yield put(addPropToStoreObject("tile", tile.id, {rackId: rack.id}));
            break;
        case "COOLING_ITEM":
            const coolingItem = yield fetchAndStoreCoolingItem(tile.objectId);
            yield put(addPropToStoreObject("tile", tile.id, {coolingItemId: coolingItem.id}));
            break;
        case "PSU":
            const psu = yield fetchAndStorePSU(tile.objectId);
            yield put(addPropToStoreObject("tile", tile.id, {psuId: psu.id}));
            break;
        default:
            console.warn("Unknown object type encountered while fetching tile objects");
    }
}

export function* onStartNewRoomConstruction() {
    try {
        const datacenterId = yield select(state => state.currentDatacenterId);
        const room = yield call(addRoomToDatacenter, {
            id: -1,
            datacenterId,
            roomType: "SERVER"
        });
        const roomWithEmptyTileList = Object.assign(room, {tileIds: []});
        yield put(addToStore("room", roomWithEmptyTileList));
        yield put(startNewRoomConstructionSucceeded(room.id));
    } catch (error) {
        console.log(error);
    }
}

export function* onCancelNewRoomConstruction() {
    try {
        const roomId = yield select(state => state.currentRoomInConstruction);
        yield call(deleteRoom, roomId);
        yield put(cancelNewRoomConstructionSucceeded());
    } catch (error) {
        console.log(error);
    }
}

export function* onAddTile(action) {
    try {
        const roomId = yield select(state => state.currentRoomInConstruction);
        const tile = yield call(addTileToRoom, {
            roomId,
            positionX: action.positionX,
            positionY: action.positionY
        });
        yield put(addToStore("tile", tile));
        yield put(addTileSucceeded(tile.id));
    } catch (error) {
        console.log(error);
    }
}

export function* onDeleteTile(action) {
    try {
        yield call(deleteTile, action.tileId);
        yield put(deleteTileSucceeded(action.tileId));
    } catch (error) {
        console.log(error);
    }
}

export function* onEditRoomName(action) {
    try {
        const roomId = yield select(state => state.interactionLevel.roomId);
        const room = Object.assign({}, yield select(state => state.objects.room[roomId]));
        room.name = action.name;
        yield call(updateRoom, room);
        yield put(editRoomNameSucceeded(action.name));
    } catch (error) {
        console.log(error);
    }
}
