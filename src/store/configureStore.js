import {applyMiddleware, compose, createStore} from "redux";
import persistState from "redux-localstorage";
import {createLogger} from "redux-logger";
import {authRedirectMiddleware} from "../auth/index";
import rootReducer from "../reducers/index";

const logger = createLogger();

const configureStore = () => createStore(
    rootReducer,
    compose(
        persistState("auth"),
        applyMiddleware(
            logger,
            authRedirectMiddleware,
        )
    )
);

export default configureStore;
