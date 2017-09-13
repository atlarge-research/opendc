import {getAll} from "./util";

export function getAllSchedulers() {
    return getAll("/schedulers");
}
