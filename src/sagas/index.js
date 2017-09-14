import {takeEvery} from "redux-saga/effects";
import {LOG_IN} from "../actions/auth";
import {ADD_EXPERIMENT, DELETE_EXPERIMENT, FETCH_EXPERIMENTS_OF_SIMULATION} from "../actions/experiments";
import {ADD_SIMULATION, DELETE_SIMULATION} from "../actions/simulations";
import {
    ADD_TILE,
    CANCEL_NEW_ROOM_CONSTRUCTION,
    DELETE_TILE,
    FETCH_LATEST_DATACENTER,
    START_NEW_ROOM_CONSTRUCTION
} from "../actions/topology/building";
import {ADD_UNIT, DELETE_MACHINE, DELETE_UNIT} from "../actions/topology/machine";
import {ADD_MACHINE, DELETE_RACK, EDIT_RACK_NAME} from "../actions/topology/rack";
import {ADD_RACK_TO_TILE, DELETE_ROOM, EDIT_ROOM_NAME} from "../actions/topology/room";
import {DELETE_CURRENT_USER, FETCH_AUTHORIZATIONS_OF_CURRENT_USER} from "../actions/users";
import {onAddExperiment, onDeleteExperiment, onFetchExperimentsOfSimulation} from "./experiments";
import {onDeleteCurrentUser} from "./profile";
import {onSimulationAdd, onSimulationDelete} from "./simulations";
import {
    onAddMachine,
    onAddRackToTile,
    onAddTile,
    onAddUnit,
    onCancelNewRoomConstruction,
    onDeleteMachine,
    onDeleteRack,
    onDeleteRoom,
    onDeleteTile,
    onDeleteUnit,
    onEditRackName,
    onEditRoomName,
    onFetchLatestDatacenter,
    onStartNewRoomConstruction
} from "./topology";
import {onFetchAuthorizationsOfCurrentUser, onFetchLoggedInUser} from "./users";

export default function* rootSaga() {
    yield takeEvery(LOG_IN, onFetchLoggedInUser);

    yield takeEvery(FETCH_AUTHORIZATIONS_OF_CURRENT_USER, onFetchAuthorizationsOfCurrentUser);
    yield takeEvery(ADD_SIMULATION, onSimulationAdd);
    yield takeEvery(DELETE_SIMULATION, onSimulationDelete);

    yield takeEvery(DELETE_CURRENT_USER, onDeleteCurrentUser);

    yield takeEvery(FETCH_LATEST_DATACENTER, onFetchLatestDatacenter);
    yield takeEvery(START_NEW_ROOM_CONSTRUCTION, onStartNewRoomConstruction);
    yield takeEvery(CANCEL_NEW_ROOM_CONSTRUCTION, onCancelNewRoomConstruction);
    yield takeEvery(ADD_TILE, onAddTile);
    yield takeEvery(DELETE_TILE, onDeleteTile);
    yield takeEvery(EDIT_ROOM_NAME, onEditRoomName);
    yield takeEvery(DELETE_ROOM, onDeleteRoom);
    yield takeEvery(EDIT_RACK_NAME, onEditRackName);
    yield takeEvery(DELETE_RACK, onDeleteRack);
    yield takeEvery(ADD_RACK_TO_TILE, onAddRackToTile);
    yield takeEvery(ADD_MACHINE, onAddMachine);
    yield takeEvery(DELETE_MACHINE, onDeleteMachine);
    yield takeEvery(ADD_UNIT, onAddUnit);
    yield takeEvery(DELETE_UNIT, onDeleteUnit);

    yield takeEvery(FETCH_EXPERIMENTS_OF_SIMULATION, onFetchExperimentsOfSimulation);
    yield takeEvery(ADD_EXPERIMENT, onAddExperiment);
    yield takeEvery(DELETE_EXPERIMENT, onDeleteExperiment);
}
