import {CacheController, CacheStatus} from "./cache";
import * as io from "socket.io-client";


export class SocketController {
    private static id = 1;
    private _socket: SocketIOClient.Socket;
    private _cacheController: CacheController;

    // Mapping from request IDs to their registered callbacks
    private callbacks: { [keys: number]: (response: IResponse) => any };


    constructor(onConnect: () => any) {
        this.callbacks = {};
        this._cacheController = new CacheController();

        this._socket = io.connect('https://opendc.ewi.tudelft.nl:443');
        this._socket.on('connect', onConnect);

        this._socket.on('response', (jsonResponse: string) => {
            let response: IResponse = JSON.parse(jsonResponse);
            console.log("Response, ID:", response.id, response);
            this.callbacks[response.id](response);
            delete this.callbacks[response.id];
        });
    }

    /**
     * Sends a request to the server socket and registers the callback to be triggered on response.
     *
     * @param request The request instance to be sent
     * @param callback A function to be called with the response object once the socket has received a response
     */
    public sendRequest(request: IRequest, callback: (response: IResponse) => any): void {
        // Check local cache, in case request is for cachable GET route
        let cacheStatus = this._cacheController.checkCache(request);

        if (cacheStatus === CacheStatus.HIT) {
            callback({
                status: {
                    code: 200
                },
                content: this._cacheController.fetchFromCache(request),
                id: -1
            });
        } else if (cacheStatus === CacheStatus.FETCHING) {
            this._cacheController.registerCallback(request, callback);
        } else if (cacheStatus === CacheStatus.MISS || cacheStatus === CacheStatus.NOT_CACHABLE) {
            if (!this._socket.connected) {
                console.error("Socket not connected, sending request failed");
            }

            if (cacheStatus === CacheStatus.MISS) {
                this._cacheController.setToFetching(request);

                this.callbacks[SocketController.id] = (response: IResponse) => {
                    this._cacheController.onFetch(request, response);
                    callback(response);
                };
            } else {
                this.callbacks[SocketController.id] = callback;
            }

            // Setup request object
            request.id = SocketController.id;
            request.token = localStorage.getItem("googleToken");
            request.path = "/v1" + request.path;

            console.log("Request, ID:", request.id, request);
            this._socket.emit("request", request);

            SocketController.id++;
        }
    }
}