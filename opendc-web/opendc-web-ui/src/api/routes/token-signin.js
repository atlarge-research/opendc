export function performTokenSignIn(token) {
    const apiUrl = process.env.NEXT_PUBLIC_API_BASE_URL

    return fetch(`${apiUrl}/tokensignin`, {
        method: 'POST',
        body: new URLSearchParams({
            idtoken: token,
        }),
    }).then((res) => res.json())
}
