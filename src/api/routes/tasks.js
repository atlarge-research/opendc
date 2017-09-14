import {getAll} from "./util";

export function getAllTasks() {
    return getAll("/tasks");
}
