import config from '../config'
import { getAuthToken } from '../auth'

const apiUrl = config['API_BASE_URL']

export async function request(path, method = 'GET', body) {
    const res = await fetch(`${apiUrl}/v2/${path}`, {
        method: method,
        headers: {
            'auth-token': getAuthToken(),
            'Content-Type': 'application/json',
        },
        body: body && JSON.stringify(body),
    })
    const { status, content } = await res.json()

    if (status.code !== 200) {
        throw status
    }

    return content
}
