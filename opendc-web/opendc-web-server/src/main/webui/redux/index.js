import { useMemo } from 'react'
import { applyMiddleware, compose, createStore } from 'redux'
import { createLogger } from 'redux-logger'
import createSagaMiddleware from 'redux-saga'
import thunk from 'redux-thunk'
import rootReducer from './reducers'
import rootSaga from './sagas'
import { createReduxEnhancer } from '@sentry/react'
import { sentryDsn } from '../config'

let store

function initStore(initialState, ctx) {
    const sagaMiddleware = createSagaMiddleware({ context: ctx })

    const middlewares = [thunk, sagaMiddleware]

    if (process.env.NODE_ENV !== 'production') {
        middlewares.push(createLogger())
    }

    let middleware = applyMiddleware(...middlewares)

    if (sentryDsn) {
        middleware = compose(middleware, createReduxEnhancer())
    }

    const configuredStore = createStore(rootReducer, initialState, middleware)
    sagaMiddleware.run(rootSaga)
    store = configuredStore

    return configuredStore
}

export const initializeStore = (preloadedState, ctx) => {
    let _store = store ?? initStore(preloadedState, ctx)

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

export function useStore(initialState, ctx) {
    return useMemo(() => initializeStore(initialState, ctx), [initialState, ctx])
}
