import {call, put, select} from "redux-saga/effects";
import {addToStore} from "../actions/objects";
import {getDatacenter, getRoomsOfDatacenter} from "../api/routes/datacenters";
import {getPath, getSectionsOfPath} from "../api/routes/paths";
import {getTilesOfRoom} from "../api/routes/rooms";
import {getAllSchedulers} from "../api/routes/schedulers";
import {getSection} from "../api/routes/sections";
import {getPathsOfSimulation, getSimulation} from "../api/routes/simulations";
import {
    getAllCPUs,
    getAllGPUs,
    getAllMemories,
    getAllStorages,
    getCoolingItem,
    getCPU,
    getFailureModel,
    getGPU,
    getMemory,
    getPSU,
    getStorage
} from "../api/routes/specifications";
import {getMachinesOfRackByTile, getRackByTile} from "../api/routes/tiles";
import {getAllTraces} from "../api/routes/traces";
import {getUser} from "../api/routes/users";

export const OBJECT_SELECTORS = {
    simulation: state => state.objects.simulation,
    user: state => state.objects.user,
    authorization: state => state.objects.authorization,
    failureModel: state => state.objects.failureModel,
    cpu: state => state.objects.cpu,
    gpu: state => state.objects.gpu,
    memory: state => state.objects.memory,
    storage: state => state.objects.storage,
    machine: state => state.objects.machine,
    rack: state => state.objects.rack,
    coolingItem: state => state.objects.coolingItem,
    psu: state => state.objects.psu,
    tile: state => state.objects.tile,
    room: state => state.objects.room,
    datacenter: state => state.objects.datacenter,
    section: state => state.objects.section,
    path: state => state.objects.path,
};

function* fetchAndStoreObject(objectType, id, apiCall) {
    const objectStore = yield select(OBJECT_SELECTORS[objectType]);
    let object = objectStore[id];
    if (!object) {
        object = yield apiCall;
        yield put(addToStore(objectType, object));
    }
    return object;
}

function* fetchAndStoreObjects(objectType, apiCall) {
    const objects = yield apiCall;
    for (let index in objects) {
        yield put(addToStore(objectType, objects[index]));
    }
    return objects;
}

export const fetchAndStoreSimulation = (id) =>
    fetchAndStoreObject("simulation", id, call(getSimulation, id));

export const fetchAndStoreUser = (id) =>
    fetchAndStoreObject("user", id, call(getUser, id));

export const fetchAndStoreFailureModel = (id) =>
    fetchAndStoreObject("failureModel", id, call(getFailureModel, id));

export const fetchAndStoreAllCPUs = () =>
    fetchAndStoreObjects("cpu", call(getAllCPUs));

export const fetchAndStoreCPU = (id) =>
    fetchAndStoreObject("cpu", id, call(getCPU, id));

export const fetchAndStoreAllGPUs = () =>
    fetchAndStoreObjects("gpu", call(getAllGPUs));

export const fetchAndStoreGPU = (id) =>
    fetchAndStoreObject("gpu", id, call(getGPU, id));

export const fetchAndStoreAllMemories = () =>
    fetchAndStoreObjects("memory", call(getAllMemories));

export const fetchAndStoreMemory = (id) =>
    fetchAndStoreObject("memory", id, call(getMemory, id));

export const fetchAndStoreAllStorages = () =>
    fetchAndStoreObjects("storage", call(getAllStorages));

export const fetchAndStoreStorage = (id) =>
    fetchAndStoreObject("storage", id, call(getStorage, id));

export const fetchAndStoreMachinesOfTile = (tileId) =>
    fetchAndStoreObjects("machine", call(getMachinesOfRackByTile, tileId));

export const fetchAndStoreRackOnTile = (id, tileId) =>
    fetchAndStoreObject("rack", id, call(getRackByTile, tileId));

export const fetchAndStoreCoolingItem = (id) =>
    fetchAndStoreObject("coolingItem", id, call(getCoolingItem, id));

export const fetchAndStorePSU = (id) =>
    fetchAndStoreObject("psu", id, call(getPSU, id));

export const fetchAndStoreTilesOfRoom = (roomId) =>
    fetchAndStoreObjects("tile", call(getTilesOfRoom, roomId));

export const fetchAndStoreRoomsOfDatacenter = (datacenterId) =>
    fetchAndStoreObjects("room", call(getRoomsOfDatacenter, datacenterId));

export const fetchAndStoreDatacenter = (id) =>
    fetchAndStoreObject("datacenter", id, call(getDatacenter, id));

export const fetchAndStoreSection = (id) =>
    fetchAndStoreObject("section", id, call(getSection, id));

export const fetchAndStoreSectionsOfPath = (pathId) =>
    fetchAndStoreObjects("section", call(getSectionsOfPath, pathId));

export const fetchAndStorePath = (id) =>
    fetchAndStoreObject("path", id, call(getPath, id));

export const fetchAndStorePathsOfSimulation = (simulationId) =>
    fetchAndStoreObjects("path", call(getPathsOfSimulation, simulationId));

export const fetchAndStoreAllTraces = () =>
    fetchAndStoreObjects("trace", call(getAllTraces));

export const fetchAndStoreAllSchedulers = function* () {
    const objects = yield call(getAllSchedulers);
    for (let index in objects) {
        objects[index].id = objects[index].name;
        yield put(addToStore("scheduler", objects[index]));
    }
    return objects;
};
