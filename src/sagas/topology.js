import {call, put, select} from "redux-saga/effects";
import {addPropToStoreObject, addToStore} from "../actions/objects";
import {
    addMachineSucceeded,
    addRackToTileSucceeded,
    addTileSucceeded,
    cancelNewRoomConstructionSucceeded,
    deleteRackSucceeded,
    deleteRoomSucceeded,
    deleteTileSucceeded,
    editRackNameSucceeded,
    editRoomNameSucceeded,
    fetchLatestDatacenterSucceeded,
    startNewRoomConstructionSucceeded
} from "../actions/topology";
import {addRoomToDatacenter} from "../api/routes/datacenters";
import {addTileToRoom, deleteRoom, updateRoom} from "../api/routes/rooms";
import {
    addMachineToRackOnTile,
    addRackToTile,
    deleteRackFromTile,
    deleteTile,
    updateRackOnTile
} from "../api/routes/tiles";
import {
    fetchAndStoreCoolingItem,
    fetchAndStoreCPU,
    fetchAndStoreDatacenter,
    fetchAndStoreGPU,
    fetchAndStoreMachinesOfTile,
    fetchAndStoreMemory,
    fetchAndStorePathsOfSimulation,
    fetchAndStorePSU,
    fetchAndStoreRackOnTile,
    fetchAndStoreRoomsOfDatacenter,
    fetchAndStoreSectionsOfPath,
    fetchAndStoreStorage,
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
            yield fetchMachinesOfRack(tile.id, rack);
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
            console.warn("Unknown rack type encountered while fetching tile objects");
    }
}

function* fetchMachinesOfRack(tileId, rack) {
    const machines = yield fetchAndStoreMachinesOfTile(tileId);
    const machineIds = new Array(rack.capacity).fill(null);
    machines.forEach(machine => machineIds[machine.position - 1] = machine.id);

    yield put(addPropToStoreObject("rack", rack.id, {machineIds}));

    for (let index in machines) {
        for (let i in machines[index].cpuIds) {
            yield fetchAndStoreCPU(machines[index].cpuIds[i]);
        }
        for (let i in machines[index].gpuIds) {
            yield fetchAndStoreGPU(machines[index].gpuIds[i]);
        }
        for (let i in machines[index].memoryIds) {
            yield fetchAndStoreMemory(machines[index].memoryIds[i]);
        }
        for (let i in machines[index].storageIds) {
            yield fetchAndStoreStorage(machines[index].storageIds[i]);
        }
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
        const roomWithEmptyTileList = Object.assign({}, room, {tileIds: []});
        yield put(addToStore("room", roomWithEmptyTileList));
        yield put(startNewRoomConstructionSucceeded(room.id));
    } catch (error) {
        console.log(error);
    }
}

export function* onCancelNewRoomConstruction() {
    try {
        const roomId = yield select(state => state.construction.currentRoomInConstruction);
        yield call(deleteRoom, roomId);
        yield put(cancelNewRoomConstructionSucceeded());
    } catch (error) {
        console.log(error);
    }
}

export function* onAddTile(action) {
    try {
        const roomId = yield select(state => state.construction.currentRoomInConstruction);
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

export function* onDeleteRoom() {
    try {
        const roomId = yield select(state => state.interactionLevel.roomId);
        yield call(deleteRoom, roomId);
        yield put(deleteRoomSucceeded());
    } catch (error) {
        console.log(error);
    }
}

export function* onEditRackName(action) {
    try {
        const tileId = yield select(state => state.interactionLevel.tileId);
        const rackId = yield select(state => state.objects.tile[state.interactionLevel.tileId].objectId);
        const rack = Object.assign({}, yield select(state => state.objects.rack[rackId]));
        rack.name = action.name;
        yield call(updateRackOnTile, tileId, rack);
        yield put(editRackNameSucceeded(action.name));
    } catch (error) {
        console.log(error);
    }
}

export function* onDeleteRack() {
    try {
        const tileId = yield select(state => state.interactionLevel.tileId);
        yield call(deleteRackFromTile, tileId);
        yield put(deleteRackSucceeded());
    } catch (error) {
        console.log(error);
    }
}

export function* onAddRackToTile(action) {
    try {
        const rack = yield call(addRackToTile, action.tileId, {
            id: -1,
            name: "Rack",
            capacity: 42,
            powerCapacityW: 100,
            machines: 20
        });
        rack.machineIds = new Array(rack.capacity).fill(null);
        yield put(addToStore("rack", rack));
        yield put(addRackToTileSucceeded(action.tileId, rack.id));
    } catch (error) {
        console.log(error);
    }
}

export function* onAddMachine(action) {
    try {
        const tileId = yield select(state => state.interactionLevel.tileId);
        const rackId = yield select(state => state.objects.tile[state.interactionLevel.tileId].objectId);
        const machine = yield call(addMachineToRackOnTile, tileId, {
            id: -1,
            rackId,
            position: action.position,
            tags: [],
            cpuIds: [],
            gpuIds: [],
            memoryIds: [],
            storageIds: [],
        });
        yield put(addToStore("machine", machine));
        yield put(addMachineSucceeded(machine));
    } catch (error) {
        console.log(error);
    }
}
