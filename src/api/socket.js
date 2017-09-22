import io from "socket.io-client";
import {getAuthToken} from "../auth/index";

let socket;
let requestIdCounter = 0;
const callbacks = {};

export function setupSocketConnection(onConnect) {
    socket = io.connect(window.location.protocol + "//" + window.location.hostname + ":8081");
    socket.on("connect", onConnect);
    socket.on("response", onSocketResponse);
}

export function sendSocketRequest(request, callback) {
    if (!socket.connected) {
        console.error("Attempted to send request over unconnected socket");
        return;
    }

    const newId = requestIdCounter++;
    callbacks[newId] = callback;

    request.id = newId;
    request.token = getAuthToken();

    if (!request.isRootRoute) {
        request.path = "/v1" + request.path;
    }

    socket.emit("request", request);

    if (process.env.NODE_ENV !== 'production') {
        console.log("Sent socket request:", request);
    }
}

function onSocketResponse(json) {
    const response = JSON.parse(json);

    if (process.env.NODE_ENV !== 'production') {
        console.log("Received socket response:", response);
    }

    callbacks[response.id](response);
    delete callbacks[response.id];
}
