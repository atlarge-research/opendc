import { takeEvery } from 'redux-saga/effects'
import { ADD_PORTFOLIO, DELETE_PORTFOLIO, OPEN_PORTFOLIO_SUCCEEDED, UPDATE_PORTFOLIO } from '../actions/portfolios'
import { ADD_PROJECT, DELETE_PROJECT, FETCH_PROJECTS, OPEN_PROJECT_SUCCEEDED } from '../actions/projects'
import {
    ADD_TILE,
    CANCEL_NEW_ROOM_CONSTRUCTION,
    DELETE_TILE,
    START_NEW_ROOM_CONSTRUCTION,
} from '../actions/topology/building'
import { ADD_UNIT, DELETE_MACHINE, DELETE_UNIT } from '../actions/topology/machine'
import { ADD_MACHINE, DELETE_RACK, EDIT_RACK_NAME } from '../actions/topology/rack'
import { ADD_RACK_TO_TILE, DELETE_ROOM, EDIT_ROOM_NAME } from '../actions/topology/room'
import { onAddPortfolio, onDeletePortfolio, onOpenPortfolioSucceeded, onUpdatePortfolio } from './portfolios'
import { onFetchProjects, onOpenProjectSucceeded, onProjectAdd, onProjectDelete } from './projects'
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
import { ADD_TOPOLOGY, DELETE_TOPOLOGY } from '../actions/topologies'
import { ADD_SCENARIO, DELETE_SCENARIO, OPEN_SCENARIO_SUCCEEDED, UPDATE_SCENARIO } from '../actions/scenarios'
import { onAddScenario, onDeleteScenario, onOpenScenarioSucceeded, onUpdateScenario } from './scenarios'
import { onAddPrefab } from './prefabs'
import { ADD_PREFAB } from '../actions/prefabs'

export default function* rootSaga() {
    yield takeEvery(FETCH_PROJECTS, onFetchProjects)
    yield takeEvery(ADD_PROJECT, onProjectAdd)
    yield takeEvery(DELETE_PROJECT, onProjectDelete)

    yield takeEvery(OPEN_PROJECT_SUCCEEDED, onOpenProjectSucceeded)
    yield takeEvery(OPEN_PORTFOLIO_SUCCEEDED, onOpenPortfolioSucceeded)
    yield takeEvery(OPEN_SCENARIO_SUCCEEDED, onOpenScenarioSucceeded)

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

    yield takeEvery(ADD_PORTFOLIO, onAddPortfolio)
    yield takeEvery(UPDATE_PORTFOLIO, onUpdatePortfolio)
    yield takeEvery(DELETE_PORTFOLIO, onDeletePortfolio)

    yield takeEvery(ADD_SCENARIO, onAddScenario)
    yield takeEvery(UPDATE_SCENARIO, onUpdateScenario)
    yield takeEvery(DELETE_SCENARIO, onDeleteScenario)

    yield takeEvery(ADD_PREFAB, onAddPrefab)
}
