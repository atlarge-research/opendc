import {combineReducers} from "redux";
import {auth} from "./auth";
import {objects} from "./objects";
import {authorizationsOfCurrentUser, authVisibilityFilter, newSimulationModalVisible} from "./simulations";

const rootReducer = combineReducers({
    auth,
    objects,
    authorizationsOfCurrentUser,
    newSimulationModalVisible,
    authVisibilityFilter,
});

export default rootReducer;
