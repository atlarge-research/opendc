import { sendSocketRequest } from './socket'

export function sendRequest(request) {
    return new Promise((resolve, reject) => {
        sendSocketRequest(request, (response) => {
            if (response.status.code === 200) {
                resolve(response.content)
            } else {
                reject(response)
            }
        })
    })
}
