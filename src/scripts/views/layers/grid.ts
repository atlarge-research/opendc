import {Layer} from "./layer";
import {MapView} from "../mapview";
import {Colors} from "../../colors";
import {CELL_SIZE} from "../../controllers/mapcontroller";


/**
 * Class responsible for rendering the grid.
 */
export class GridLayer implements Layer {
    public container: createjs.Container;
    public gridPixelSize: number;

    private mapView: MapView;
    private gridLineWidth: number;


    constructor(mapView: MapView) {
        this.mapView = mapView;
        this.container = new createjs.Container();

        this.gridLineWidth = 0.5;
        this.gridPixelSize = MapView.MAP_SIZE * CELL_SIZE;

        this.draw();
    }

    /**
     * Draws the entire grid (later to be navigated around with offsets).
     */
    public draw(): void {
        this.container.removeAllChildren();

        let currentCellX = 0;
        let currentCellY = 0;

        while (currentCellX <= MapView.MAP_SIZE) {
            MapView.drawLine(
                currentCellX * CELL_SIZE, 0,
                currentCellX * CELL_SIZE, MapView.MAP_SIZE * CELL_SIZE,
                this.gridLineWidth, Colors.GRID_COLOR, this.container);

            currentCellX++;
        }

        while (currentCellY <= MapView.MAP_SIZE) {
            MapView.drawLine(
                0, currentCellY * CELL_SIZE,
                MapView.MAP_SIZE * CELL_SIZE, currentCellY * CELL_SIZE,
                this.gridLineWidth, Colors.GRID_COLOR, this.container);

            currentCellY++;
        }
    }

    public setVisibility(value: boolean): void {
        this.container.visible = value;
    }
}