import {MapController, CELL_SIZE} from "./mapcontroller";
import {MapView} from "../views/mapview";


export class ScaleIndicatorController {
    private static MIN_WIDTH = 50;
    private static MAX_WIDTH = 100;

    private mapController: MapController;
    private mapView: MapView;

    private jqueryObject: JQuery;
    private currentDivisor: number;


    constructor(mapController: MapController) {
        this.mapController = mapController;
        this.mapView = mapController.mapView;
    }

    public init(jqueryObject: JQuery): void {
        this.jqueryObject = jqueryObject;
        this.currentDivisor = 1;
    }

    public update(): void {
        const currentZoom = this.mapView.mapContainer.scaleX;
        let newWidth;
        do {
            newWidth = (currentZoom * CELL_SIZE) / this.currentDivisor;

            if (newWidth < ScaleIndicatorController.MIN_WIDTH) {
                this.currentDivisor /= 2;
            } else if (newWidth > ScaleIndicatorController.MAX_WIDTH) {
                this.currentDivisor *= 2;
            } else {
                break;
            }
        } while (true);


        this.jqueryObject.text(MapView.CELL_SIZE_METERS / this.currentDivisor + "m");
        this.jqueryObject.width(newWidth);
    }
}
