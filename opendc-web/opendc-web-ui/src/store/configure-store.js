import { applyMiddleware, compose, createStore } from 'redux'
import persistState from 'redux-localstorage'
import { createLogger } from 'redux-logger'
import createSagaMiddleware from 'redux-saga'
import thunk from 'redux-thunk'
import { authRedirectMiddleware } from '../auth/index'
import rootReducer from '../reducers/index'
import rootSaga from '../sagas/index'
import { viewportAdjustmentMiddleware } from './middlewares/viewport-adjustment'

const sagaMiddleware = createSagaMiddleware()

const middlewares = [thunk, sagaMiddleware, authRedirectMiddleware, viewportAdjustmentMiddleware]

if (process.env.NODE_ENV !== 'production') {
    middlewares.push(createLogger())
}

export let store = undefined

export default function configureStore() {
    const configuredStore = createStore(rootReducer, compose(persistState('auth'), applyMiddleware(...middlewares)))
    sagaMiddleware.run(rootSaga)
    store = configuredStore

    return configuredStore
}
