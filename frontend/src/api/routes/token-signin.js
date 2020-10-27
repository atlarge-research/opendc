export function performTokenSignIn(token) {
    const apiUrl = process.env.REACT_APP_API_BASE_URL || ''
    const data = new FormData()
    data.append('idtoken', token)

    return fetch(`https://${apiUrl}/tokensignin`, {
        method: 'POST',
        body: data,
    })
}
