import {sendRequest} from "../index";

export function addSimulation(simulation) {
    return sendRequest({
        path: "/simulations",
        method: "POST",
        parameters: {
            body: {
                simulation
            },
            path: {},
            query: {}
        }
    });
}

export function getSimulation(simulationId) {
    return sendRequest({
        path: "/simulations/{simulationId}",
        method: "GET",
        parameters: {
            body: {},
            path: {
                simulationId
            },
            query: {}
        }
    });
}

export function updateSimulation(simulation) {
    return sendRequest({
        path: "/simulations/{simulationId}",
        method: "PUT",
        parameters: {
            body: {
                simulation
            },
            path: {
                simulationId: simulation.id
            },
            query: {}
        }
    });
}

export function deleteSimulation(simulationId) {
    return sendRequest({
        path: "/simulations/{simulationId}",
        method: "DELETE",
        parameters: {
            body: {},
            path: {
                simulationId
            },
            query: {}
        }
    });
}

export function getAuthorizationsBySimulation(simulationId) {
    return sendRequest({
        path: "/simulations/{simulationId}/authorizations",
        method: "GET",
        parameters: {
            body: {},
            path: {
                simulationId
            },
            query: {}
        }
    })
}
