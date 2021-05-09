import config from '../../config'

export function performTokenSignIn(token) {
    const apiUrl = config['API_BASE_URL']

    return fetch(`${apiUrl}/tokensignin`, {
        method: 'POST',
        body: new URLSearchParams({
            idtoken: token,
        }),
    }).then((res) => res.json())
}
