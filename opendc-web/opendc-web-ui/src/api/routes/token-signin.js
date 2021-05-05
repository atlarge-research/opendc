export function performTokenSignIn(token) {
    const apiUrl = window.$$env['API_BASE_URL'] || ''

    return fetch(`${apiUrl}/tokensignin`, {
        method: 'POST',
        body: new URLSearchParams({
            idtoken: token,
        }),
    }).then((res) => res.json())
}
