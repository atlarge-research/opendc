import { combineReducers } from "redux";
import { ADD_BATCH_TO_STATES } from "../actions/states";

export const states = combineReducers({
  task: objectStates("task"),
  room: objectStates("room"),
  rack: objectStates("rack"),
  machine: objectStates("machine")
});

function objectStates(type) {
  return (state = {}, action) => {
    if (action.objectType !== type) {
      return state;
    }

    if (action.type === ADD_BATCH_TO_STATES) {
      const batch = {};
      for (let i in action.objects) {
        batch[action.objects[i].tick] = Object.assign(
          {},
          state[action.objects[i].tick],
          batch[action.objects[i].tick],
          { [action.objects[i][action.objectType + "Id"]]: action.objects[i] }
        );
      }

      return Object.assign({}, state, batch);
    }

    return state;
  };
}
