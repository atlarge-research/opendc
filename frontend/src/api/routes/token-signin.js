export function performTokenSignIn(token) {
    const apiUrl = process.env.REACT_APP_API_BASE_URL || ''

    return fetch(`${apiUrl}/tokensignin`, {
        method: 'POST',
        body: new URLSearchParams({
            idtoken: token,
        }),
    }).then((res) => res.json())
}
