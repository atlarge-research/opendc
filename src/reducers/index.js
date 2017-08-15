import {combineReducers} from "redux";
import {auth} from "./auth";
import {objects} from "./objects";
import {authorizationsOfCurrentUser, authVisibilityFilter, newProjectModalVisible} from "./projects";

const rootReducer = combineReducers({
    auth,
    objects,
    authorizationsOfCurrentUser,
    newProjectModalVisible,
    authVisibilityFilter,
});

export default rootReducer;
