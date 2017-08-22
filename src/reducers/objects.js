import {combineReducers} from "redux";
import {ADD_TO_STORE} from "../actions/objects";

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
        if (action.type === ADD_TO_STORE) {
            if (action.objectType === type) {
                return Object.assign(
                    state,
                    {[getId(action.object)]: action.object}
                );
            }
            return state;
        }
    };
}
