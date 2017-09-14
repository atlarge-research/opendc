import {getAll, getById} from "./util";

export function getAllJobs() {
    return getAll("/jobs");
}

export function getTasksOfJob(jobId) {
    return getById("/jobs/{jobId}/tasks", {jobId});
}
