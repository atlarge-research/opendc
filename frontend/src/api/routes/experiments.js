import { deleteById, getById } from './util'
import { sendRequest } from '../index'

export function addExperiment(projectId, experiment) {
    return sendRequest({
        path: '/projects/{projectId}/experiments',
        method: 'POST',
        parameters: {
            body: {
                experiment,
            },
            path: {
                projectId,
            },
            query: {},
        },
    })
}

export function getExperiment(experimentId) {
    return getById('/experiments/{experimentId}', { experimentId })
}

export function deleteExperiment(experimentId) {
    return deleteById('/experiments/{experimentId}', { experimentId })
}
