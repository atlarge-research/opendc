import {put} from "redux-saga/effects";
import {addPropToStoreObject} from "../actions/objects";
import {fetchLatestDatacenterSucceeded} from "../actions/topology";
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
        const datacenter = yield fetchAndStoreDatacenter(datacenterId);
        datacenter.roomIds = (yield fetchAndStoreRoomsOfDatacenter(datacenterId)).map(room => room.id);

        for (let index in datacenter.roomIds) {
            yield fetchRoom(datacenter.roomIds[index]);
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
    console.log(tile);
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
