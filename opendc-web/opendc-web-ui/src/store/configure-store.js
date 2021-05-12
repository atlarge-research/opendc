import { useMemo } from 'react'
import { applyMiddleware, compose, createStore } from 'redux'
import { createLogger } from 'redux-logger'
import persistState from 'redux-localstorage'
import createSagaMiddleware from 'redux-saga'
import thunk from 'redux-thunk'
import { authRedirectMiddleware } from '../auth/index'
import rootReducer from '../reducers/index'
import rootSaga from '../sagas/index'
import { viewportAdjustmentMiddleware } from './middlewares/viewport-adjustment'

let store

function initStore(initialState) {
    const sagaMiddleware = createSagaMiddleware()

    const middlewares = [thunk, sagaMiddleware, authRedirectMiddleware, viewportAdjustmentMiddleware]

    if (process.env.NODE_ENV !== 'production') {
        middlewares.push(createLogger())
    }

    let enhancer = applyMiddleware(...middlewares)

    if (global.localStorage) {
        enhancer = compose(persistState('auth'), enhancer)
    }

    const configuredStore = createStore(rootReducer, enhancer)
    sagaMiddleware.run(rootSaga)
    store = configuredStore

    return configuredStore
}

export const initializeStore = (preloadedState) => {
    let _store = store ?? initStore(preloadedState)

    // After navigating to a page with an initial Redux state, merge that state
    // with the current state in the store, and create a new store
    if (preloadedState && store) {
        _store = initStore({
            ...store.getState(),
            ...preloadedState,
        })
        // Reset the current store
        store = undefined
    }

    // For SSG and SSR always create a new store
    if (typeof window === 'undefined') return _store
    // Create the store once in the client
    if (!store) store = _store

    return _store
}

export function useStore(initialState) {
    return useMemo(() => initializeStore(initialState), [initialState])
}
