import { takeEvery } from 'redux-saga/effects'
import { LOG_IN } from '../actions/auth'
import {
    ADD_EXPERIMENT,
    DELETE_EXPERIMENT,
    FETCH_EXPERIMENTS_OF_SIMULATION,
    OPEN_EXPERIMENT_SUCCEEDED,
} from '../actions/experiments'
import { ADD_SIMULATION, DELETE_SIMULATION, OPEN_SIMULATION_SUCCEEDED } from '../actions/simulations'
import {
    ADD_TILE,
    CANCEL_NEW_ROOM_CONSTRUCTION,
    DELETE_TILE,
    START_NEW_ROOM_CONSTRUCTION,
} from '../actions/topology/building'
import { ADD_UNIT, DELETE_MACHINE, DELETE_UNIT } from '../actions/topology/machine'
import { ADD_MACHINE, DELETE_RACK, EDIT_RACK_NAME } from '../actions/topology/rack'
import { ADD_RACK_TO_TILE, DELETE_ROOM, EDIT_ROOM_NAME } from '../actions/topology/room'
import { DELETE_CURRENT_USER, FETCH_AUTHORIZATIONS_OF_CURRENT_USER } from '../actions/users'
import {
    onAddExperiment,
    onDeleteExperiment,
    onFetchExperimentsOfSimulation,
    onOpenExperimentSucceeded,
} from './experiments'
import { onDeleteCurrentUser } from './profile'
import { onOpenSimulationSucceeded, onSimulationAdd, onSimulationDelete } from './simulations'
import {
    onAddMachine,
    onAddRackToTile,
    onAddTile,
    onAddTopology,
    onAddUnit,
    onCancelNewRoomConstruction,
    onDeleteMachine,
    onDeleteRack,
    onDeleteRoom,
    onDeleteTile,
    onDeleteTopology,
    onDeleteUnit,
    onEditRackName,
    onEditRoomName,
    onStartNewRoomConstruction,
} from './topology'
import { onFetchAuthorizationsOfCurrentUser, onFetchLoggedInUser } from './users'
import { ADD_TOPOLOGY, DELETE_TOPOLOGY } from '../actions/topologies'

export default function* rootSaga() {
    yield takeEvery(LOG_IN, onFetchLoggedInUser)

    yield takeEvery(FETCH_AUTHORIZATIONS_OF_CURRENT_USER, onFetchAuthorizationsOfCurrentUser)
    yield takeEvery(ADD_SIMULATION, onSimulationAdd)
    yield takeEvery(DELETE_SIMULATION, onSimulationDelete)

    yield takeEvery(DELETE_CURRENT_USER, onDeleteCurrentUser)

    yield takeEvery(OPEN_SIMULATION_SUCCEEDED, onOpenSimulationSucceeded)
    yield takeEvery(OPEN_EXPERIMENT_SUCCEEDED, onOpenExperimentSucceeded)

    yield takeEvery(ADD_TOPOLOGY, onAddTopology)
    yield takeEvery(DELETE_TOPOLOGY, onDeleteTopology)
    yield takeEvery(START_NEW_ROOM_CONSTRUCTION, onStartNewRoomConstruction)
    yield takeEvery(CANCEL_NEW_ROOM_CONSTRUCTION, onCancelNewRoomConstruction)
    yield takeEvery(ADD_TILE, onAddTile)
    yield takeEvery(DELETE_TILE, onDeleteTile)
    yield takeEvery(EDIT_ROOM_NAME, onEditRoomName)
    yield takeEvery(DELETE_ROOM, onDeleteRoom)
    yield takeEvery(EDIT_RACK_NAME, onEditRackName)
    yield takeEvery(DELETE_RACK, onDeleteRack)
    yield takeEvery(ADD_RACK_TO_TILE, onAddRackToTile)
    yield takeEvery(ADD_MACHINE, onAddMachine)
    yield takeEvery(DELETE_MACHINE, onDeleteMachine)
    yield takeEvery(ADD_UNIT, onAddUnit)
    yield takeEvery(DELETE_UNIT, onDeleteUnit)

    yield takeEvery(FETCH_EXPERIMENTS_OF_SIMULATION, onFetchExperimentsOfSimulation)
    yield takeEvery(ADD_EXPERIMENT, onAddExperiment)
    yield takeEvery(DELETE_EXPERIMENT, onDeleteExperiment)
}
