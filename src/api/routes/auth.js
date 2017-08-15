export function performTokenSignIn(token) {
    return new Promise((resolve, reject) => {
        window["jQuery"].post(
            "/tokensignin",
            {idtoken: token},
            data => resolve(data)
        )
    });
}
