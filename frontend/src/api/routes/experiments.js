import { deleteById, getById } from './util'
import { sendRequest } from '../index'

export function addExperiment(simulationId, experiment) {
    return sendRequest({
        path: '/simulations/{simulationId}/experiments',
        method: 'POST',
        parameters: {
            body: {
                experiment,
            },
            path: {
                simulationId,
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

export function getAllMachineStates(experimentId) {
    return getById('/experiments/{experimentId}/machine-states', {
        experimentId,
    })
}

export function getAllRackStates(experimentId) {
    return getById('/experiments/{experimentId}/rack-states', { experimentId })
}

export function getAllRoomStates(experimentId) {
    return getById('/experiments/{experimentId}/room-states', { experimentId })
}
