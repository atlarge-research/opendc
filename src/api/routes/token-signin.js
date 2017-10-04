export function performTokenSignIn(token) {
  return new Promise(resolve => {
    window["jQuery"].post("/tokensignin", { idtoken: token }, data =>
      resolve(data)
    );
  });
}
