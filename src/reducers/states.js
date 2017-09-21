import {combineReducers} from "redux";
import {ADD_TO_STATES} from "../actions/states";

export const states = combineReducers({
    task: objectStates("task"),
    room: objectStates("room"),
    rack: objectStates("rack"),
    machine: objectStates("machine"),
});

function objectStates(type) {
    return (state = {}, action) => {
        if (action.objectType !== type) {
            return state;
        }

        if (action.type === ADD_TO_STATES) {
            return Object.assign(
                {},
                state,
                {
                    [action.tick]: Object.assign(
                        {},
                        state[action.tick],
                        {[action.object[action.objectType + "Id"]]: action.object}
                    )
                }
            );
        }

        return state;
    };
}
