import {MapView} from "../mapview";
import {Colors} from "../../colors";
import {Util} from "../../util";
import {Layer} from "./layer";
import {CELL_SIZE} from "../../controllers/mapcontroller";


export class RoomTextLayer implements Layer {
    private static TEXT_PADDING = 4;

    public container: createjs.Container;

    private mapView: MapView;


    constructor(mapView: MapView) {
        this.mapView = mapView;
        this.container = new createjs.Container();

        this.draw();
    }

    public draw(): void {
        this.container.removeAllChildren();

        this.mapView.currentDatacenter.rooms.forEach((room: IRoom) => {
            if (room.name !== "" && room.roomType !== "") {
                this.renderTextOverlay(room);
            }
        });
    }

    public setVisibility(value: boolean): void {
        this.mapView.animate(this.container, {alpha: value === true ? 1 : 0});
    }

    /**
     * Draws a name and type overlay over the given room.
     */
    private renderTextOverlay(room: IRoom): void {
        if (room.name === null || room.tiles.length === 0) {
            return;
        }

        const textPos = Util.calculateRoomNamePosition(room);

        const bottomY = this.renderText(room.name, "12px Arial", textPos,
            textPos.topLeft.y * CELL_SIZE + RoomTextLayer.TEXT_PADDING);
        this.renderText("Type: " + Util.toSentenceCase(room.roomType), "10px Arial", textPos, bottomY + 5);
    }

    private renderText(text: string, font: string, textPos: IRoomNamePos, startY: number): number {
        const name = new createjs.Text(text, font, Colors.ROOM_NAME_COLOR);

        if (name.getMeasuredWidth() > textPos.length * CELL_SIZE - RoomTextLayer.TEXT_PADDING * 2) {
            name.scaleX = name.scaleY = (textPos.length * CELL_SIZE - RoomTextLayer.TEXT_PADDING * 2) /
                name.getMeasuredWidth();
        }

        // Position the text to the top left of the selected tile
        name.x = textPos.topLeft.x * CELL_SIZE + RoomTextLayer.TEXT_PADDING;
        name.y = startY;

        this.container.addChild(name);

        return name.y + name.getMeasuredHeight() * name.scaleY;
    }
}
