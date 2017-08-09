import {applyMiddleware, createStore} from "redux";
import {createLogger} from "redux-logger";
import thunkMiddleware from "redux-thunk";
import rootReducer from "../reducers/index";

const logger = createLogger();

const configureStore = () => createStore(
    rootReducer,
    applyMiddleware(
        thunkMiddleware,
        logger,
    )
);

export default configureStore;
