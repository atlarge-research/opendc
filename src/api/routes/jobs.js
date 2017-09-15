import {getById} from "./util";

export function getTasksOfJob(jobId) {
    return getById("/jobs/{jobId}/tasks", {jobId});
}
