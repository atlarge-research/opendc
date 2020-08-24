import { combineReducers } from "redux";
import {
  SET_MAP_DIMENSIONS,
  SET_MAP_POSITION,
  SET_MAP_SCALE
} from "../actions/map";

export function position(state = { x: 0, y: 0 }, action) {
  switch (action.type) {
    case SET_MAP_POSITION:
      return { x: action.x, y: action.y };
    default:
      return state;
  }
}

export function dimensions(state = { width: 600, height: 400 }, action) {
  switch (action.type) {
    case SET_MAP_DIMENSIONS:
      return { width: action.width, height: action.height };
    default:
      return state;
  }
}

export function scale(state = 1, action) {
  switch (action.type) {
    case SET_MAP_SCALE:
      return action.scale;
    default:
      return state;
  }
}

export const map = combineReducers({
  position,
  dimensions,
  scale
});
