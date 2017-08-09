import {combineReducers} from "redux";
import {authorizations, authVisibilityFilter, newProjectModalVisible} from "./projects";

const rootReducer = combineReducers({
    authorizations,
    newProjectModalVisible,
    authVisibilityFilter,
});

export default rootReducer;
