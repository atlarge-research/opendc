import {Colors} from "../../colors";
import {MapView} from "../mapview";
import {Util} from "../../util";
import {Layer} from "./layer";
import {CELL_SIZE} from "../../controllers/mapcontroller";


/**
 * Class responsible for rendering the walls.
 */
export class WallLayer implements Layer {
    public container: createjs.Container;

    private mapView: MapView;
    private walls: IRoomWall[];
    private wallLineWidth: number;


    constructor(mapView: MapView) {
        this.mapView = mapView;
        this.container = new createjs.Container();
        this.wallLineWidth = CELL_SIZE / 20.0;

        this.generateWalls();
        this.draw();
    }

    /**
     * Calls the Util.deriveWallLocations function to generate the wall locations.
     */
    public generateWalls(): void {
        this.walls = Util.deriveWallLocations(this.mapView.currentDatacenter.rooms);
    }

    /**
     * Draws all walls to the canvas.
     */
    public draw(): void {
        this.container.removeAllChildren();

        // Draw walls
        this.walls.forEach((element: IRoomWall) => {
            if (element.horizontal) {
                MapView.drawLine(
                    CELL_SIZE * element.startPos[0] - this.wallLineWidth / 2.0,
                    CELL_SIZE * element.startPos[1],
                    CELL_SIZE * (element.startPos[0] + element.length) + this.wallLineWidth / 2.0,
                    CELL_SIZE * element.startPos[1],
                    this.wallLineWidth, Colors.WALL_COLOR, this.container
                );
            } else {
                MapView.drawLine(
                    CELL_SIZE * element.startPos[0],
                    CELL_SIZE * element.startPos[1] - this.wallLineWidth / 2.0,
                    CELL_SIZE * element.startPos[0],
                    CELL_SIZE * (element.startPos[1] + element.length) + this.wallLineWidth / 2.0,
                    this.wallLineWidth, Colors.WALL_COLOR, this.container
                );
            }
        });
    }
}