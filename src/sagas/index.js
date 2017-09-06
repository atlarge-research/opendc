import {takeEvery} from "redux-saga/effects";
import {LOG_IN} from "../actions/auth";
import {ADD_SIMULATION, DELETE_SIMULATION} from "../actions/simulations";
import {
    ADD_RACK_TO_TILE,
    ADD_TILE,
    CANCEL_NEW_ROOM_CONSTRUCTION,
    DELETE_ROOM,
    DELETE_TILE,
    EDIT_ROOM_NAME,
    FETCH_LATEST_DATACENTER,
    START_NEW_ROOM_CONSTRUCTION
} from "../actions/topology";
import {DELETE_CURRENT_USER, FETCH_AUTHORIZATIONS_OF_CURRENT_USER} from "../actions/users";
import {onDeleteCurrentUser} from "./profile";
import {onSimulationAdd, onSimulationDelete} from "./simulations";
import {
    onAddRackToTile,
    onAddTile,
    onCancelNewRoomConstruction,
    onDeleteRoom,
    onDeleteTile,
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
    yield takeEvery(ADD_RACK_TO_TILE, onAddRackToTile);
}
