import { combineReducers } from 'redux'
import {
    ADD_ID_TO_STORE_OBJECT_LIST_PROP,
    ADD_PROP_TO_STORE_OBJECT,
    ADD_TO_STORE,
    REMOVE_ID_FROM_STORE_OBJECT_LIST_PROP,
} from '../actions/objects'
import { CPU_UNITS, GPU_UNITS, MEMORY_UNITS, STORAGE_UNITS } from '../../util/unit-specifications'
import { STORE_TOPOLOGY } from '../actions/topologies'

export const objects = combineReducers({
    cpu: object('cpu', CPU_UNITS),
    gpu: object('gpu', GPU_UNITS),
    memory: object('memory', MEMORY_UNITS),
    storage: object('storage', STORAGE_UNITS),
    machine: object('machine'),
    rack: object('rack'),
    tile: object('tile'),
    room: object('room'),
    topology: object('topology'),
    prefab: object('prefab'),
})

function object(type, defaultState = {}) {
    return objectWithId(type, (object) => object._id, defaultState)
}

function objectWithId(type, getId, defaultState = {}) {
    return (state = defaultState, action) => {
        if (action.type === STORE_TOPOLOGY) {
            return { ...state, ...action.entities[type] }
        } else if (action.objectType !== type) {
            return state
        }

        if (action.type === ADD_TO_STORE) {
            return { ...state, [getId(action.object)]: action.object }
        } else if (action.type === ADD_PROP_TO_STORE_OBJECT) {
            return { ...state, [action.objectId]: { ...state[action.objectId], ...action.propObject } }
        } else if (action.type === ADD_ID_TO_STORE_OBJECT_LIST_PROP) {
            return Object.assign({}, state, {
                [action.objectId]: Object.assign({}, state[action.objectId], {
                    [action.propName]: [...state[action.objectId][action.propName], action.id],
                }),
            })
        } else if (action.type === REMOVE_ID_FROM_STORE_OBJECT_LIST_PROP) {
            return Object.assign({}, state, {
                [action.objectId]: Object.assign({}, state[action.objectId], {
                    [action.propName]: state[action.objectId][action.propName].filter((id) => id !== action.id),
                }),
            })
        }

        return state
    }
}
