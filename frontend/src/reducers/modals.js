import { combineReducers } from 'redux'
import { CLOSE_DELETE_PROFILE_MODAL, OPEN_DELETE_PROFILE_MODAL } from '../actions/modals/profile'
import { CLOSE_NEW_PROJECT_MODAL, OPEN_NEW_PROJECT_MODAL } from '../actions/modals/projects'
import {
    CLOSE_NEW_TOPOLOGY_MODAL,
    CLOSE_DELETE_MACHINE_MODAL,
    CLOSE_DELETE_RACK_MODAL,
    CLOSE_DELETE_ROOM_MODAL,
    CLOSE_EDIT_RACK_NAME_MODAL,
    CLOSE_EDIT_ROOM_NAME_MODAL,
    OPEN_NEW_TOPOLOGY_MODAL,
    OPEN_DELETE_MACHINE_MODAL,
    OPEN_DELETE_RACK_MODAL,
    OPEN_DELETE_ROOM_MODAL,
    OPEN_EDIT_RACK_NAME_MODAL,
    OPEN_EDIT_ROOM_NAME_MODAL,
} from '../actions/modals/topology'
import { CLOSE_NEW_PORTFOLIO_MODAL, OPEN_NEW_PORTFOLIO_MODAL } from '../actions/modals/portfolios'
import { CLOSE_NEW_SCENARIO_MODAL, OPEN_NEW_SCENARIO_MODAL } from '../actions/modals/scenarios'

function modal(openAction, closeAction) {
    return function (state = false, action) {
        switch (action.type) {
            case openAction:
                return true
            case closeAction:
                return false
            default:
                return state
        }
    }
}

export const modals = combineReducers({
    newProjectModalVisible: modal(OPEN_NEW_PROJECT_MODAL, CLOSE_NEW_PROJECT_MODAL),
    deleteProfileModalVisible: modal(OPEN_DELETE_PROFILE_MODAL, CLOSE_DELETE_PROFILE_MODAL),
    changeTopologyModalVisible: modal(OPEN_NEW_TOPOLOGY_MODAL, CLOSE_NEW_TOPOLOGY_MODAL),
    editRoomNameModalVisible: modal(OPEN_EDIT_ROOM_NAME_MODAL, CLOSE_EDIT_ROOM_NAME_MODAL),
    deleteRoomModalVisible: modal(OPEN_DELETE_ROOM_MODAL, CLOSE_DELETE_ROOM_MODAL),
    editRackNameModalVisible: modal(OPEN_EDIT_RACK_NAME_MODAL, CLOSE_EDIT_RACK_NAME_MODAL),
    deleteRackModalVisible: modal(OPEN_DELETE_RACK_MODAL, CLOSE_DELETE_RACK_MODAL),
    deleteMachineModalVisible: modal(OPEN_DELETE_MACHINE_MODAL, CLOSE_DELETE_MACHINE_MODAL),
    newPortfolioModalVisible: modal(OPEN_NEW_PORTFOLIO_MODAL, CLOSE_NEW_PORTFOLIO_MODAL),
    newScenarioModalVisible: modal(OPEN_NEW_SCENARIO_MODAL, CLOSE_NEW_SCENARIO_MODAL),
})
