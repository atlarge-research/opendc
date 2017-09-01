import {combineReducers} from "redux";
import {
    ADD_ID_TO_STORE_OBJECT_LIST_PROP,
    ADD_PROP_TO_STORE_OBJECT,
    ADD_TO_STORE,
    REMOVE_ID_FROM_STORE_OBJECT_LIST_PROP
} from "../actions/objects";

export const objects = combineReducers({
    simulation: object("simulation"),
    user: object("user"),
    authorization: objectWithId("authorization", object => [object.userId, object.simulationId]),
    failureModel: object("failureModel"),
    cpu: object("cpu"),
    gpu: object("gpu"),
    memory: object("memory"),
    storage: object("storage"),
    machine: object("machine"),
    rack: object("rack"),
    coolingItem: object("coolingItem"),
    psu: object("psu"),
    tile: object("tile"),
    room: object("room"),
    datacenter: object("datacenter"),
    section: object("section"),
    path: object("path"),
});

function object(type) {
    return objectWithId(type, object => object.id);
}

function objectWithId(type, getId) {
    return (state = {}, action) => {
        if (action.objectType !== type) {
            return state;
        }

        if (action.type === ADD_TO_STORE) {
            return Object.assign(
                {},
                state,
                {[getId(action.object)]: action.object}
            );
        } else if (action.type === ADD_PROP_TO_STORE_OBJECT) {
            return Object.assign(
                {},
                state,
                {
                    [action.objectId]: Object.assign(
                        {},
                        state[action.objectId],
                        action.propObject
                    )
                }
            );
        } else if (action.type === ADD_ID_TO_STORE_OBJECT_LIST_PROP) {
            return Object.assign(
                {},
                state,
                {
                    [action.objectId]: Object.assign(
                        {},
                        state[action.objectId],
                        {[action.propName]: [...state[action.objectId][action.propName], action.id]}
                    )
                }
            );
        } else if (action.type === REMOVE_ID_FROM_STORE_OBJECT_LIST_PROP) {
            return Object.assign(
                {},
                state,
                {
                    [action.objectId]: Object.assign(
                        {},
                        state[action.objectId],
                        {[action.propName]: state[action.objectId][action.propName].filter(id => id !== action.id)}
                    )
                }
            );
        }

        return state;
    };
}
