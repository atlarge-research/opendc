import { sendRequest } from '../index'
import { getById } from './util'

export function getDatacenter(datacenterId) {
    return getById('/datacenters/{datacenterId}', { datacenterId })
}

export function getRoomsOfDatacenter(datacenterId) {
    return getById('/datacenters/{datacenterId}/rooms', { datacenterId })
}

export function addRoomToDatacenter(room) {
    return sendRequest({
        path: '/datacenters/{datacenterId}/rooms',
        method: 'POST',
        parameters: {
            body: {
                room,
            },
            path: {
                datacenterId: room.datacenterId,
            },
            query: {},
        },
    })
}
