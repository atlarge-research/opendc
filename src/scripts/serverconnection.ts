import {SocketController} from "./controllers/connection/socket";


export class ServerConnection {
    private static _socketControllerInstance: SocketController;


    public static connect(onConnect: () => any): void {
        this._socketControllerInstance = new SocketController(onConnect);
    }

    public static send(request: IRequest): Promise<any> {
        return new Promise((resolve, reject) => {
            let checkUnimplemented = ServerConnection.interceptUnimplementedEndpoint(request);
            if (checkUnimplemented) {
                resolve(checkUnimplemented.content);
                return;
            }

            this._socketControllerInstance.sendRequest(request, (response: IResponse) => {
                if (response.status.code === 200) {
                    ServerConnection.convertFlatToNestedPositionData(response.content, resolve);
                } else {
                    reject(response.status);
                }
            });
        })
    }

    public static convertFlatToNestedPositionData(responseContent, resolve): void {
        let nestPositionCoords = (content: any) => {
            if (content["positionX"] !== undefined) {
                content["position"] = {
                    x: content["positionX"],
                    y: content["positionY"]
                };
            }
        };

        if (responseContent instanceof Array) {
            responseContent.forEach(nestPositionCoords);
        } else {
            nestPositionCoords(responseContent);
        }

        resolve(responseContent);
    }

    /**
     * Intercepts endpoints that are still unimplemented and responds with mock data.
     *
     * @param request The request
     * @returns {any} A response, or null if the endpoint is not on the list of unimplemented ones.
     */
    public static interceptUnimplementedEndpoint(request: IRequest): IResponse {
        // Endpoints that are unimplemented can be intercepted here
        return null;
    }
}