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
} from '../../components/app/map/MapConstants'
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

export function* onAddTopology(action) {
    try {
        const { projectId, duplicateId, name } = action

        let topologyToBeCreated
        if (duplicateId) {
            topologyToBeCreated = yield denormalizeTopology(duplicateId)
            topologyToBeCreated = { ...topologyToBeCreated, name }
            delete topologyToBeCreated._id
        } else {
            topologyToBeCreated = { name: action.name, rooms: [] }
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

export function* onEditRoomName(action) {
    try {
        const topologyId = yield select((state) => state.currentTopologyId)
        const roomId = yield select((state) => state.interactionLevel.roomId)
        const room = Object.assign({}, yield select((state) => state.objects.room[roomId]))
        room.name = action.name
        yield put(addPropToStoreObject('room', roomId, { name: action.name }))
        yield updateTopologyOnServer(topologyId)
    } catch (error) {
        console.error(error)
    }
}

export function* onDeleteRoom() {
    try {
        const topologyId = yield select((state) => state.currentTopologyId)
        const roomId = yield select((state) => state.interactionLevel.roomId)
        yield put(goDownOneInteractionLevel())
        yield put(removeIdFromStoreObjectListProp('topology', topologyId, 'rooms', roomId))
        yield updateTopologyOnServer(topologyId)
    } catch (error) {
        console.error(error)
    }
}

export function* onEditRackName(action) {
    try {
        const topologyId = yield select((state) => state.currentTopologyId)
        const rackId = yield select((state) => state.objects.tile[state.interactionLevel.tileId].rack)
        const rack = Object.assign({}, yield select((state) => state.objects.rack[rackId]))
        rack.name = action.name
        yield put(addPropToStoreObject('rack', rackId, { name: action.name }))
        yield updateTopologyOnServer(topologyId)
    } catch (error) {
        console.error(error)
    }
}

export function* onDeleteRack() {
    try {
        const topologyId = yield select((state) => state.currentTopologyId)
        const tileId = yield select((state) => state.interactionLevel.tileId)
        yield put(goDownOneInteractionLevel())
        yield put(addPropToStoreObject('tile', tileId, { rack: undefined }))
        yield updateTopologyOnServer(topologyId)
    } catch (error) {
        console.error(error)
    }
}

export function* onAddRackToTile(action) {
    try {
        const topologyId = yield select((state) => state.currentTopologyId)
        const rack = {
            _id: uuid(),
            name: 'Rack',
            capacity: DEFAULT_RACK_SLOT_CAPACITY,
            powerCapacityW: DEFAULT_RACK_POWER_CAPACITY,
            machines: [],
        }
        yield put(addToStore('rack', rack))
        yield put(addPropToStoreObject('tile', action.tileId, { rack: rack._id }))
        yield updateTopologyOnServer(topologyId)
    } catch (error) {
        console.error(error)
    }
}

export function* onAddMachine(action) {
    try {
        const topologyId = yield select((state) => state.currentTopologyId)
        const rackId = yield select((state) => state.objects.tile[state.interactionLevel.tileId].rack)
        const rack = yield select((state) => state.objects.rack[rackId])

        const machine = {
            _id: uuid(),
            position: action.position,
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

export function* onDeleteMachine() {
    try {
        const topologyId = yield select((state) => state.currentTopologyId)
        const tileId = yield select((state) => state.interactionLevel.tileId)
        const position = yield select((state) => state.interactionLevel.position)
        const rack = yield select((state) => state.objects.rack[state.objects.tile[tileId].rack])
        yield put(goDownOneInteractionLevel())
        yield put(
            addPropToStoreObject('rack', rack._id, { machines: rack.machines.filter((_, idx) => idx !== position - 1) })
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

export function* onAddUnit(action) {
    try {
        const topologyId = yield select((state) => state.currentTopologyId)
        const tileId = yield select((state) => state.interactionLevel.tileId)
        const position = yield select((state) => state.interactionLevel.position)
        const machine = yield select(
            (state) => state.objects.machine[state.objects.rack[state.objects.tile[tileId].rack].machines[position - 1]]
        )

        if (machine[unitMapping[action.unitType]].length >= MAX_NUM_UNITS_PER_MACHINE) {
            return
        }

        const units = [...machine[unitMapping[action.unitType]], action.id]
        yield put(
            addPropToStoreObject('machine', machine._id, {
                [unitMapping[action.unitType]]: units,
            })
        )
        yield updateTopologyOnServer(topologyId)
    } catch (error) {
        console.error(error)
    }
}

export function* onDeleteUnit(action) {
    try {
        const topologyId = yield select((state) => state.currentTopologyId)
        const tileId = yield select((state) => state.interactionLevel.tileId)
        const position = yield select((state) => state.interactionLevel.position)
        const machine = yield select(
            (state) => state.objects.machine[state.objects.rack[state.objects.tile[tileId].rack].machines[position - 1]]
        )
        const unitIds = machine[unitMapping[action.unitType]].slice()
        unitIds.splice(action.index, 1)

        yield put(
            addPropToStoreObject('machine', machine._id, {
                [unitMapping[action.unitType]]: unitIds,
            })
        )
        yield updateTopologyOnServer(topologyId)
    } catch (error) {
        console.error(error)
    }
}
