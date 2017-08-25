import {combineReducers} from "redux";
import {auth} from "./auth";
import {modals} from "./modals";
import {objects} from "./objects";
import {authorizationsOfCurrentUser, authVisibilityFilter, currentSimulationId} from "./simulations";
import {currentDatacenterId} from "./topology";

const rootReducer = combineReducers({
    auth,
    objects,
    modals,
    authorizationsOfCurrentUser,
    authVisibilityFilter,
    currentSimulationId,
    currentDatacenterId,
});

export default rootReducer;
