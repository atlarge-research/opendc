import {combineReducers} from "redux";
import {auth} from "./auth";
import {authorizations, authVisibilityFilter, newProjectModalVisible} from "./projects";

const rootReducer = combineReducers({
    auth,
    authorizations,
    newProjectModalVisible,
    authVisibilityFilter,
});

export default rootReducer;
