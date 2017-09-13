import {getAll, getById} from "./util";

export function getAllTraces() {
    return getAll("/traces");
}

export function getJobsOfTrace(traceId) {
    return getById("/traces/{traceId}/jobs", {traceId});
}
