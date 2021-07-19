import { call, put, select, getContext } from 'redux-saga/effects'
import { goDownOneInteractionLevel } from '../actions/interaction-level'
import {
    addIdToStoreObjectListProp,
    addPropToStoreObject,
    addToStore,
    removeIdFromStoreObjectListProp,
} from '../actions/objects'
import {
    cancelNewRoomConstructionSucceeded,
    setCurrentTopology,
    startNewRoomConstructionSucceeded,
} from '../actions/topology/building'
import {
    DEFAULT_RACK_POWER_CAPACITY,
    DEFAULT_RACK_SLOT_CAPACITY,
    MAX_NUM_UNITS_PER_MACHINE,
} from '../../components/topologies/map/MapConstants'
import { fetchAndStoreTopology, denormalizeTopology, updateTopologyOnServer } from './objects'
import { uuid } from 'uuidv4'
import { addTopology } from '../../api/topologies'

export function* fetchAndStoreAllTopologiesOfProject(projectId, setTopology = false) {
    try {
        const queryClient = yield getContext('queryClient')
        const project = yield call(() => queryClient.fetchQuery(['projects', projectId]))

        for (const id of project.topologyIds) {
            yield fetchAndStoreTopology(id)
        }

        if (setTopology) {
            yield put(setCurrentTopology(project.topologyIds[0]))
        }
    } catch (error) {
        console.error(error)
    }
}

export function* onAddTopology({ projectId, duplicateId, name }) {
    try {
        let topologyToBeCreated
        if (duplicateId) {
            topologyToBeCreated = yield denormalizeTopology(duplicateId)
            topologyToBeCreated = { ...topologyToBeCreated, name }
            delete topologyToBeCreated._id
        } else {
            topologyToBeCreated = { name, rooms: [] }
        }

        const auth = yield getContext('auth')
        const topology = yield call(addTopology, auth, { ...topologyToBeCreated, projectId })
        yield fetchAndStoreTopology(topology._id)
        yield put(setCurrentTopology(topology._id))
    } catch (error) {
        console.error(error)
    }
}

export function* onStartNewRoomConstruction() {
    try {
        const topologyId = yield select((state) => state.currentTopologyId)
        const room = {
            _id: uuid(),
            name: 'Room',
            topologyId,
            tiles: [],
        }
        yield put(addToStore('room', room))
        yield put(addIdToStoreObjectListProp('topology', topologyId, 'rooms', room._id))
        yield updateTopologyOnServer(topologyId)
        yield put(startNewRoomConstructionSucceeded(room._id))
    } catch (error) {
        console.error(error)
    }
}

export function* onCancelNewRoomConstruction() {
    try {
        const topologyId = yield select((state) => state.currentTopologyId)
        const roomId = yield select((state) => state.construction.currentRoomInConstruction)
        yield put(removeIdFromStoreObjectListProp('topology', topologyId, 'rooms', roomId))
        // TODO remove room from store, too
        yield updateTopologyOnServer(topologyId)
        yield put(cancelNewRoomConstructionSucceeded())
    } catch (error) {
        console.error(error)
    }
}

export function* onAddTile(action) {
    try {
        const topologyId = yield select((state) => state.currentTopologyId)
        const roomId = yield select((state) => state.construction.currentRoomInConstruction)
        const tile = {
            _id: uuid(),
            roomId,
            positionX: action.positionX,
            positionY: action.positionY,
        }
        yield put(addToStore('tile', tile))
        yield put(addIdToStoreObjectListProp('room', roomId, 'tiles', tile._id))
        yield updateTopologyOnServer(topologyId)
    } catch (error) {
        console.error(error)
    }
}

export function* onDeleteTile(action) {
    try {
        const topologyId = yield select((state) => state.currentTopologyId)
        const roomId = yield select((state) => state.construction.currentRoomInConstruction)
        yield put(removeIdFromStoreObjectListProp('room', roomId, 'tiles', action.tileId))
        yield updateTopologyOnServer(topologyId)
    } catch (error) {
        console.error(error)
    }
}

export function* onEditRoomName({ roomId, name }) {
    try {
        const topologyId = yield select((state) => state.currentTopologyId)
        yield put(addPropToStoreObject('room', roomId, { name }))
        yield updateTopologyOnServer(topologyId)
    } catch (error) {
        console.error(error)
    }
}

export function* onDeleteRoom({ roomId }) {
    try {
        const topologyId = yield select((state) => state.currentTopologyId)
        yield put(goDownOneInteractionLevel())
        yield put(removeIdFromStoreObjectListProp('topology', topologyId, 'rooms', roomId))
        yield updateTopologyOnServer(topologyId)
    } catch (error) {
        console.error(error)
    }
}

export function* onEditRackName({ rackId, name }) {
    try {
        const topologyId = yield select((state) => state.currentTopologyId)
        yield put(addPropToStoreObject('rack', rackId, { name }))
        yield updateTopologyOnServer(topologyId)
    } catch (error) {
        console.error(error)
    }
}

export function* onDeleteRack({ tileId }) {
    try {
        const topologyId = yield select((state) => state.currentTopologyId)
        yield put(goDownOneInteractionLevel())
        yield put(addPropToStoreObject('tile', tileId, { rack: undefined }))
        yield updateTopologyOnServer(topologyId)
    } catch (error) {
        console.error(error)
    }
}

export function* onAddRackToTile({ tileId }) {
    try {
        const topologyId = yield select((state) => state.currentTopologyId)
        const rack = {
            _id: uuid(),
            name: 'Rack',
            tileId,
            capacity: DEFAULT_RACK_SLOT_CAPACITY,
            powerCapacityW: DEFAULT_RACK_POWER_CAPACITY,
            machines: [],
        }
        yield put(addToStore('rack', rack))
        yield put(addPropToStoreObject('tile', tileId, { rack: rack._id }))
        yield updateTopologyOnServer(topologyId)
    } catch (error) {
        console.error(error)
    }
}

export function* onAddMachine({ rackId, position }) {
    try {
        const topologyId = yield select((state) => state.currentTopologyId)
        const rack = yield select((state) => state.objects.rack[rackId])

        const machine = {
            _id: uuid(),
            rackId,
            position,
            cpus: [],
            gpus: [],
            memories: [],
            storages: [],
        }
        yield put(addToStore('machine', machine))

        const machineIds = [...rack.machines, machine._id]
        yield put(addPropToStoreObject('rack', rackId, { machines: machineIds }))
        yield updateTopologyOnServer(topologyId)
    } catch (error) {
        console.error(error)
    }
}

export function* onDeleteMachine({ rackId, position }) {
    try {
        const topologyId = yield select((state) => state.currentTopologyId)
        const rack = yield select((state) => state.objects.rack[rackId])
        yield put(goDownOneInteractionLevel())
        yield put(
            addPropToStoreObject('rack', rackId, { machines: rack.machines.filter((_, idx) => idx !== position - 1) })
        )
        yield updateTopologyOnServer(topologyId)
    } catch (error) {
        console.error(error)
    }
}

const unitMapping = {
    cpu: 'cpus',
    gpu: 'gpus',
    memory: 'memories',
    storage: 'storages',
}

export function* onAddUnit({ machineId, unitType, id }) {
    try {
        const topologyId = yield select((state) => state.currentTopologyId)
        const machine = yield select((state) => state.objects.machine[machineId])

        if (machine[unitMapping[unitType]].length >= MAX_NUM_UNITS_PER_MACHINE) {
            return
        }

        const units = [...machine[unitMapping[unitType]], id]
        yield put(
            addPropToStoreObject('machine', machine._id, {
                [unitMapping[unitType]]: units,
            })
        )
        yield updateTopologyOnServer(topologyId)
    } catch (error) {
        console.error(error)
    }
}

export function* onDeleteUnit({ machineId, unitType, index }) {
    try {
        const topologyId = yield select((state) => state.currentTopologyId)
        const machine = yield select((state) => state.objects.machine[machineId])
        const unitIds = machine[unitMapping[unitType]].slice()
        unitIds.splice(index, 1)

        yield put(
            addPropToStoreObject('machine', machine._id, {
                [unitMapping[unitType]]: unitIds,
            })
        )
        yield updateTopologyOnServer(topologyId)
    } catch (error) {
        console.error(error)
    }
}
