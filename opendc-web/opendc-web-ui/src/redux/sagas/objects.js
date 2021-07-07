import { call, put, select, getContext } from 'redux-saga/effects'
import { addToStore } from '../actions/objects'
import { fetchSchedulers } from '../../api/schedulers'
import { fetchTraces } from '../../api/traces'
import { getTopology, updateTopology } from '../../api/topologies'
import { uuid } from 'uuidv4'

export const OBJECT_SELECTORS = {
    user: (state) => state.objects.user,
    authorization: (state) => state.objects.authorization,
    portfolio: (state) => state.objects.portfolio,
    scenario: (state) => state.objects.scenario,
    cpu: (state) => state.objects.cpu,
    gpu: (state) => state.objects.gpu,
    memory: (state) => state.objects.memory,
    storage: (state) => state.objects.storage,
    machine: (state) => state.objects.machine,
    rack: (state) => state.objects.rack,
    tile: (state) => state.objects.tile,
    room: (state) => state.objects.room,
    topology: (state) => state.objects.topology,
}

function* fetchAndStoreObject(objectType, id, apiCall) {
    const objectStore = yield select(OBJECT_SELECTORS[objectType])
    let object = objectStore[id]
    if (!object) {
        object = yield apiCall
        yield put(addToStore(objectType, object))
    }
    return object
}

function* fetchAndStoreObjects(objectType, apiCall) {
    const objects = yield apiCall
    for (let object of objects) {
        yield put(addToStore(objectType, object))
    }
    return objects
}

export const fetchAndStoreTopology = function* (id) {
    const topologyStore = yield select(OBJECT_SELECTORS['topology'])
    const roomStore = yield select(OBJECT_SELECTORS['room'])
    const tileStore = yield select(OBJECT_SELECTORS['tile'])
    const rackStore = yield select(OBJECT_SELECTORS['rack'])
    const machineStore = yield select(OBJECT_SELECTORS['machine'])
    const auth = yield getContext('auth')

    let topology = topologyStore[id]
    if (!topology) {
        const fullTopology = yield call(getTopology, auth, id)

        for (let roomIdx in fullTopology.rooms) {
            const fullRoom = fullTopology.rooms[roomIdx]

            generateIdIfNotPresent(fullRoom)

            if (!roomStore[fullRoom._id]) {
                for (let tileIdx in fullRoom.tiles) {
                    const fullTile = fullRoom.tiles[tileIdx]

                    generateIdIfNotPresent(fullTile)

                    if (!tileStore[fullTile._id]) {
                        if (fullTile.rack) {
                            const fullRack = fullTile.rack

                            generateIdIfNotPresent(fullRack)

                            if (!rackStore[fullRack._id]) {
                                for (let machineIdx in fullRack.machines) {
                                    const fullMachine = fullRack.machines[machineIdx]

                                    generateIdIfNotPresent(fullMachine)

                                    if (!machineStore[fullMachine._id]) {
                                        let machine = (({ _id, position, cpus, gpus, memories, storages }) => ({
                                            _id,
                                            rackId: fullRack._id,
                                            position,
                                            cpuIds: cpus.map((u) => u._id),
                                            gpuIds: gpus.map((u) => u._id),
                                            memoryIds: memories.map((u) => u._id),
                                            storageIds: storages.map((u) => u._id),
                                        }))(fullMachine)
                                        yield put(addToStore('machine', machine))
                                    }
                                }

                                const filledSlots = new Array(fullRack.capacity).fill(null)
                                fullRack.machines.forEach(
                                    (machine) => (filledSlots[machine.position - 1] = machine._id)
                                )
                                let rack = (({ _id, name, capacity, powerCapacityW }) => ({
                                    _id,
                                    name,
                                    capacity,
                                    powerCapacityW,
                                    machineIds: filledSlots,
                                }))(fullRack)
                                yield put(addToStore('rack', rack))
                            }
                        }

                        let tile = (({ _id, positionX, positionY, rack }) => ({
                            _id,
                            roomId: fullRoom._id,
                            positionX,
                            positionY,
                            rackId: rack ? rack._id : undefined,
                        }))(fullTile)
                        yield put(addToStore('tile', tile))
                    }
                }

                let room = (({ _id, name, tiles }) => ({ _id, name, tileIds: tiles.map((t) => t._id) }))(fullRoom)
                yield put(addToStore('room', room))
            }
        }

        topology = (({ _id, name, rooms }) => ({ _id, name, roomIds: rooms.map((r) => r._id) }))(fullTopology)
        yield put(addToStore('topology', topology))

        // TODO consider pushing the IDs
    }

    return topology
}

const generateIdIfNotPresent = (obj) => {
    if (!obj._id) {
        obj._id = uuid()
    }
}

export const updateTopologyOnServer = function* (id) {
    const topology = yield getTopologyAsObject(id, true)
    const auth = yield getContext('auth')
    yield call(updateTopology, auth, topology)
}

export const getTopologyAsObject = function* (id, keepIds) {
    const topologyStore = yield select(OBJECT_SELECTORS['topology'])
    const rooms = yield getAllRooms(topologyStore[id].roomIds, keepIds)
    return {
        _id: keepIds ? id : undefined,
        name: topologyStore[id].name,
        rooms: rooms,
    }
}

export const getAllRooms = function* (roomIds, keepIds) {
    const roomStore = yield select(OBJECT_SELECTORS['room'])

    let rooms = []

    for (let id of roomIds) {
        let tiles = yield getAllRoomTiles(roomStore[id], keepIds)
        rooms.push({
            _id: keepIds ? id : undefined,
            name: roomStore[id].name,
            tiles: tiles,
        })
    }
    return rooms
}

export const getAllRoomTiles = function* (roomStore, keepIds) {
    let tiles = []

    for (let id of roomStore.tileIds) {
        tiles.push(yield getTileById(id, keepIds))
    }
    return tiles
}

export const getTileById = function* (id, keepIds) {
    const tileStore = yield select(OBJECT_SELECTORS['tile'])
    return {
        _id: keepIds ? id : undefined,
        positionX: tileStore[id].positionX,
        positionY: tileStore[id].positionY,
        rack: !tileStore[id].rackId ? undefined : yield getRackById(tileStore[id].rackId, keepIds),
    }
}

export const getRackById = function* (id, keepIds) {
    const rackStore = yield select(OBJECT_SELECTORS['rack'])
    const machineStore = yield select(OBJECT_SELECTORS['machine'])
    const cpuStore = yield select(OBJECT_SELECTORS['cpu'])
    const gpuStore = yield select(OBJECT_SELECTORS['gpu'])
    const memoryStore = yield select(OBJECT_SELECTORS['memory'])
    const storageStore = yield select(OBJECT_SELECTORS['storage'])

    return {
        _id: keepIds ? rackStore[id]._id : undefined,
        name: rackStore[id].name,
        capacity: rackStore[id].capacity,
        powerCapacityW: rackStore[id].powerCapacityW,
        machines: rackStore[id].machineIds
            .filter((m) => m !== null)
            .map((machineId) => ({
                _id: keepIds ? machineId : undefined,
                position: machineStore[machineId].position,
                cpus: machineStore[machineId].cpuIds.map((id) => cpuStore[id]),
                gpus: machineStore[machineId].gpuIds.map((id) => gpuStore[id]),
                memories: machineStore[machineId].memoryIds.map((id) => memoryStore[id]),
                storages: machineStore[machineId].storageIds.map((id) => storageStore[id]),
            })),
    }
}
