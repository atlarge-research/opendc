export enum CacheStatus {
    MISS,
    FETCHING,
    HIT,
    NOT_CACHABLE
}


interface ICachableObject {
    status: CacheStatus;
    object: any;
    callbacks: any[];
}


export class CacheController {
    private static CACHABLE_ROUTES = [
        "/specifications/psus/{id}",
        "/specifications/cooling-items/{id}",
        "/specifications/cpus/{id}",
        "/specifications/gpus/{id}",
        "/specifications/memories/{id}",
        "/specifications/storages/{id}",
        "/specifications/failure-models/{id}",
    ];

    // Maps every route name to a map of IDs => objects
    private routeCaches: { [keys: string]: { [keys: number]: ICachableObject } };


    constructor() {
        this.routeCaches = {};

        CacheController.CACHABLE_ROUTES.forEach((routeName: string) => {
            this.routeCaches[routeName] = {};
        })
    }

    public checkCache(request: IRequest): CacheStatus {
        if (request.method === "GET" && CacheController.CACHABLE_ROUTES.indexOf(request.path) !== -1) {
            if (this.routeCaches[request.path][request.parameters.path["id"]] === undefined) {
                this.routeCaches[request.path][request.parameters.path["id"]] = {
                    status: CacheStatus.MISS,
                    object: null,
                    callbacks: []
                };
                return CacheStatus.MISS;
            } else {
                return this.routeCaches[request.path][request.parameters.path["id"]].status;
            }
        } else {
            return CacheStatus.NOT_CACHABLE;
        }
    }

    public fetchFromCache(request: IRequest): any {
        return this.routeCaches[request.path][request.parameters.path["id"]].object;
    }

    public setToFetching(request: IRequest): void {
        this.routeCaches[request.path][request.parameters.path["id"]].status = CacheStatus.FETCHING;
    }

    public onFetch(request: IRequest, response: IResponse): any {
        let pathWithoutVersion = request.path.replace(/\/v\d+/, "");
        this.routeCaches[pathWithoutVersion][request.parameters.path["id"]].status = CacheStatus.HIT;
        this.routeCaches[pathWithoutVersion][request.parameters.path["id"]].object = response.content;

        this.routeCaches[pathWithoutVersion][request.parameters.path["id"]].callbacks.forEach((callback) => {
            callback({
                status: {
                    code: 200
                },
                content: response.content,
                id: request.id
            });
        });

        this.routeCaches[pathWithoutVersion][request.parameters.path["id"]].callbacks = [];
    }

    public registerCallback(request: IRequest, callback): any {
        this.routeCaches[request.path][request.parameters.path["id"]].callbacks.push(callback);
    }
}
