import {deleteById} from "./util";

export function deleteExperiment(experimentId) {
    return deleteById("/experiments/{experimentId}", {experimentId});
}
