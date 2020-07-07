import { call, put, select } from 'redux-saga/effects'
import { addToStore } from '../actions/objects'
import { getAllSchedulers } from '../api/routes/schedulers'
import { getProject } from '../api/routes/projects'
import { getAllTraces } from '../api/routes/traces'
import { getUser } from '../api/routes/users'
import { getTopology, updateTopology } from '../api/routes/topologies'
import { uuid } from 'uuidv4'

export const OBJECT_SELECTORS = {
    project: (state) => state.objects.project,
    user: (state) => state.objects.user,
    authorization: (state) => state.objects.authorization,
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
    for (let index in objects) {
        yield put(addToStore(objectType, objects[index]))
    }
    return objects
}

export const fetchAndStoreProject = (id) => fetchAndStoreObject('project', id, call(getProject, id))

export const fetchAndStoreUser = (id) => fetchAndStoreObject('user', id, call(getUser, id))

export const fetchAndStoreTopology = function* (id) {
    const topologyStore = yield select(OBJECT_SELECTORS['topology'])
    const roomStore = yield select(OBJECT_SELECTORS['room'])
    const tileStore = yield select(OBJECT_SELECTORS['tile'])
    const rackStore = yield select(OBJECT_SELECTORS['rack'])
    const machineStore = yield select(OBJECT_SELECTORS['machine'])

    let topology = topologyStore[id]
    if (!topology) {
        const fullTopology = yield call(getTopology, id)

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
                                    (machine) => (filledSlots[machine.position - 1] = machine._id),
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

        console.log('Full topology after insertion', fullTopology)
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
    const topologyStore = yield select(OBJECT_SELECTORS['topology'])
    const roomStore = yield select(OBJECT_SELECTORS['room'])
    const tileStore = yield select(OBJECT_SELECTORS['tile'])
    const rackStore = yield select(OBJECT_SELECTORS['rack'])
    const machineStore = yield select(OBJECT_SELECTORS['machine'])
    const cpuStore = yield select(OBJECT_SELECTORS['cpu'])
    const gpuStore = yield select(OBJECT_SELECTORS['gpu'])
    const memoryStore = yield select(OBJECT_SELECTORS['memory'])
    const storageStore = yield select(OBJECT_SELECTORS['storage'])

    const topology = {
        _id: id,
        name: topologyStore[id].name,
        rooms: topologyStore[id].roomIds.map((roomId) => ({
            _id: roomId,
            name: roomStore[roomId].name,
            tiles: roomStore[roomId].tileIds.map((tileId) => ({
                _id: tileId,
                positionX: tileStore[tileId].positionX,
                positionY: tileStore[tileId].positionY,
                rack: !tileStore[tileId].rackId
                    ? undefined
                    : {
                        _id: rackStore[tileStore[tileId].rackId]._id,
                        name: rackStore[tileStore[tileId].rackId].name,
                        capacity: rackStore[tileStore[tileId].rackId].capacity,
                        powerCapacityW: rackStore[tileStore[tileId].rackId].powerCapacityW,
                        machines: rackStore[tileStore[tileId].rackId].machineIds
                            .filter((m) => m !== null)
                            .map((machineId) => ({
                                _id: machineId,
                                position: machineStore[machineId].position,
                                cpus: machineStore[machineId].cpuIds.map((id) => cpuStore[id]),
                                gpus: machineStore[machineId].gpuIds.map((id) => gpuStore[id]),
                                memories: machineStore[machineId].memoryIds.map((id) => memoryStore[id]),
                                storages: machineStore[machineId].storageIds.map((id) => storageStore[id]),
                            })),
                    },
            })),
        })),
    }

    yield call(updateTopology, topology)
}

export const fetchAndStoreAllTraces = () => fetchAndStoreObjects('trace', call(getAllTraces))

export const fetchAndStoreAllSchedulers = function* () {
    const objects = yield call(getAllSchedulers)
    for (let index in objects) {
        objects[index]._id = objects[index].name
        yield put(addToStore('scheduler', objects[index]))
    }
    return objects
}
