import {call, put, select} from "redux-saga/effects";
import {goDownOneInteractionLevel} from "../actions/interaction-level";
import {
    addIdToStoreObjectListProp,
    addPropToStoreObject,
    addToStore,
    removeIdFromStoreObjectListProp
} from "../actions/objects";
import {
    cancelNewRoomConstructionSucceeded,
    fetchLatestDatacenterSucceeded,
    startNewRoomConstructionSucceeded
} from "../actions/topology/building";
import {addRoomToDatacenter} from "../api/routes/datacenters";
import {addTileToRoom, deleteRoom, updateRoom} from "../api/routes/rooms";
import {
    addMachineToRackOnTile,
    addRackToTile,
    deleteMachineInRackOnTile,
    deleteRackFromTile,
    deleteTile,
    updateMachineInRackOnTile,
    updateRackOnTile
} from "../api/routes/tiles";
import {
    DEFAULT_RACK_POWER_CAPACITY,
    DEFAULT_RACK_SLOT_CAPACITY,
    MAX_NUM_UNITS_PER_MACHINE
} from "../components/map/MapConstants";
import {
    fetchAndStoreAllCPUs,
    fetchAndStoreAllGPUs,
    fetchAndStoreAllMemories,
    fetchAndStoreAllStorages,
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
        yield fetchAllUnitSpecifications();
        yield fetchDatacenter(latestSection.datacenterId);
        yield put(fetchLatestDatacenterSucceeded(latestSection.datacenterId));
    } catch (error) {
        console.error(error);
    }
}

function* fetchDatacenter(datacenterId) {
    try {
        yield fetchAndStoreDatacenter(datacenterId);
        const rooms = yield fetchAndStoreRoomsOfDatacenter(datacenterId);
        yield put(addPropToStoreObject("datacenter", datacenterId, {roomIds: rooms.map(room => room.id)}));

        for (let index in rooms) {
            yield fetchRoom(rooms[index].id);
        }
    } catch (error) {
        console.error(error);
    }
}

function* fetchAllUnitSpecifications() {
    try {
        yield fetchAndStoreAllCPUs();
        yield fetchAndStoreAllGPUs();
        yield fetchAndStoreAllMemories();
        yield fetchAndStoreAllStorages();
    } catch (error) {
        console.error(error);
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
        yield put(addIdToStoreObjectListProp("datacenter", datacenterId, "roomIds", room.id));
        yield put(startNewRoomConstructionSucceeded(room.id));
    } catch (error) {
        console.error(error);
    }
}

export function* onCancelNewRoomConstruction() {
    try {
        const datacenterId = yield select(state => state.currentDatacenterId);
        const roomId = yield select(state => state.construction.currentRoomInConstruction);
        yield call(deleteRoom, roomId);
        yield put(removeIdFromStoreObjectListProp("datacenter", datacenterId, "roomIds", roomId));
        yield put(cancelNewRoomConstructionSucceeded());
    } catch (error) {
        console.error(error);
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
        yield put(addIdToStoreObjectListProp("room", roomId, "tileIds", tile.id));
    } catch (error) {
        console.error(error);
    }
}

export function* onDeleteTile(action) {
    try {
        const roomId = yield select(state => state.construction.currentRoomInConstruction);
        yield call(deleteTile, action.tileId);
        yield put(removeIdFromStoreObjectListProp("room", roomId, "tileIds", action.tileId));
    } catch (error) {
        console.error(error);
    }
}

export function* onEditRoomName(action) {
    try {
        const roomId = yield select(state => state.interactionLevel.roomId);
        const room = Object.assign({}, yield select(state => state.objects.room[roomId]));
        room.name = action.name;
        yield call(updateRoom, room);
        yield put(addPropToStoreObject("room", roomId, {name: action.name}));
    } catch (error) {
        console.error(error);
    }
}

export function* onDeleteRoom() {
    try {
        const datacenterId = yield select(state => state.currentDatacenterId);
        const roomId = yield select(state => state.interactionLevel.roomId);
        yield call(deleteRoom, roomId);
        yield put(goDownOneInteractionLevel());
        yield put(removeIdFromStoreObjectListProp("datacenter", datacenterId, "roomIds", roomId));
    } catch (error) {
        console.error(error);
    }
}

export function* onEditRackName(action) {
    try {
        const tileId = yield select(state => state.interactionLevel.tileId);
        const rackId = yield select(state => state.objects.tile[state.interactionLevel.tileId].objectId);
        const rack = Object.assign({}, yield select(state => state.objects.rack[rackId]));
        rack.name = action.name;
        yield call(updateRackOnTile, tileId, rack);
        yield put(addPropToStoreObject("rack", rackId, {name: action.name}));
    } catch (error) {
        console.error(error);
    }
}

export function* onDeleteRack() {
    try {
        const tileId = yield select(state => state.interactionLevel.tileId);
        yield call(deleteRackFromTile, tileId);
        yield put(goDownOneInteractionLevel());
        yield put(addPropToStoreObject("tile", tileId, {objectType: undefined}));
        yield put(addPropToStoreObject("tile", tileId, {objectId: undefined}));
    } catch (error) {
        console.error(error);
    }
}

export function* onAddRackToTile(action) {
    try {
        const rack = yield call(addRackToTile, action.tileId, {
            id: -1,
            name: "Rack",
            capacity: DEFAULT_RACK_SLOT_CAPACITY,
            powerCapacityW: DEFAULT_RACK_POWER_CAPACITY
        });
        rack.machineIds = new Array(rack.capacity).fill(null);
        yield put(addToStore("rack", rack));
        yield put(addPropToStoreObject("tile", action.tileId, {objectId: rack.id}));
        yield put(addPropToStoreObject("tile", action.tileId, {objectType: "RACK"}));
    } catch (error) {
        console.error(error);
    }
}

export function* onAddMachine(action) {
    try {
        const tileId = yield select(state => state.interactionLevel.tileId);
        const rackId = yield select(state => state.objects.tile[state.interactionLevel.tileId].objectId);
        const rack = yield select(state => state.objects.rack[rackId]);

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

        const machineIds = [...rack.machineIds];
        machineIds[machine.position - 1] = machine.id;
        yield put(addPropToStoreObject("rack", rackId, {machineIds}));
    } catch (error) {
        console.error(error);
    }
}

export function* onDeleteMachine() {
    try {
        const tileId = yield select(state => state.interactionLevel.tileId);
        const position = yield select(state => state.interactionLevel.position);
        const rack = yield select(state => state.objects.rack[state.objects.tile[tileId].objectId]);
        yield call(deleteMachineInRackOnTile, tileId, position);
        const machineIds = [...rack.machineIds];
        machineIds[position - 1] = null;
        yield put(goDownOneInteractionLevel());
        yield put(addPropToStoreObject("rack", rack.id, {machineIds}));
    } catch (error) {
        console.error(error);
    }
}

export function* onAddUnit(action) {
    try {
        const tileId = yield select(state => state.interactionLevel.tileId);
        const position = yield select(state => state.interactionLevel.position);
        const machine = yield select(state => state.objects.machine[state.objects.rack[
            state.objects.tile[tileId].objectId].machineIds[position - 1]]);

        if (machine[action.unitType + "Ids"].length >= MAX_NUM_UNITS_PER_MACHINE) {
            return;
        }

        const units = [...machine[action.unitType + "Ids"], action.id];
        const updatedMachine = Object.assign({}, machine,
            {[action.unitType + "Ids"]: units});

        yield call(updateMachineInRackOnTile, tileId, position, updatedMachine);

        yield put(addPropToStoreObject("machine", machine.id, {[action.unitType + "Ids"]: units}));
    } catch (error) {
        console.error(error);
    }
}

export function* onDeleteUnit(action) {
    try {
        const tileId = yield select(state => state.interactionLevel.tileId);
        const position = yield select(state => state.interactionLevel.position);
        const machine = yield select(state => state.objects.machine[state.objects.rack[
            state.objects.tile[tileId].objectId].machineIds[position - 1]]);
        const unitIds = machine[action.unitType + "Ids"].slice();
        unitIds.splice(action.index, 1);
        const updatedMachine = Object.assign({}, machine, {[action.unitType + "Ids"]: unitIds});

        yield call(updateMachineInRackOnTile, tileId, position, updatedMachine);
        yield put(addPropToStoreObject("machine", machine.id, {[action.unitType + "Ids"]: unitIds}));
    } catch (error) {
        console.error(error);
    }
}
