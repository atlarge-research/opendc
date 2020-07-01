import { sendRequest } from '../index'
import { deleteById, getById } from './util'

export function getSimulation(simulationId) {
    return getById('/simulations/{simulationId}', { simulationId })
}

export function addSimulation(simulation) {
    return sendRequest({
        path: '/simulations',
        method: 'POST',
        parameters: {
            body: {
                simulation,
            },
            path: {},
            query: {},
        },
    })
}

export function updateSimulation(simulation) {
    return sendRequest({
        path: '/simulations/{simulationId}',
        method: 'PUT',
        parameters: {
            body: {
                simulation,
            },
            path: {
                simulationId: simulation._id,
            },
            query: {},
        },
    })
}

export function deleteSimulation(simulationId) {
    return deleteById('/simulations/{simulationId}', { simulationId })
}

export function getAuthorizationsBySimulation(simulationId) {
    return getById('/simulations/{simulationId}/authorizations', {
        simulationId,
    })
}

export function getPathsOfSimulation(simulationId) {
    return getById('/simulations/{simulationId}/paths', { simulationId })
}

export function getExperimentsOfSimulation(simulationId) {
    return getById('/simulations/{simulationId}/experiments', { simulationId })
}

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
