import {combineReducers} from "redux";
import {auth} from "./auth";
import {modals} from "./modals";
import {objects} from "./objects";
import {authorizationsOfCurrentUser, authVisibilityFilter} from "./simulations";

const rootReducer = combineReducers({
    auth,
    objects,
    modals,
    authorizationsOfCurrentUser,
    authVisibilityFilter,
});

export default rootReducer;
