import {DCObjectLayer} from "./dcobject";
import {CELL_SIZE} from "../../controllers/mapcontroller";


export class DCProgressBar {
    public static PROGRESS_BAR_WIDTH = CELL_SIZE / 7.0;

    public container: createjs.Container;
    public fillRatio: number;

    private backgroundRect: createjs.Shape;
    private backgroundColor: string;
    private fillRect: createjs.Shape;
    private fillColor: string;
    private bitmap: createjs.Bitmap;
    private position: IGridPosition;
    private distanceFromBottom: number;


    /**
     * Draws a progress rectangle with rounded ends.
     *
     * @param position The coordinates of the grid cell in which it should be located
     * @param color The background color of the item
     * @param container The container to which it should be drawn
     * @param distanceFromBottom The index of its vertical position, counted from the bottom (0 is the lowest position)
     * @param fractionFilled The fraction of the available horizontal space that the progress bar should take up
     * @returns {createjs.Shape} The drawn shape
     */
    public static drawItemProgressRectangle(position: IGridPosition, color: string,
                                            container: createjs.Container, distanceFromBottom: number,
                                            fractionFilled: number): createjs.Shape {
        let shape = new createjs.Shape();
        shape.graphics.beginFill(color);
        let x = position.x * CELL_SIZE + DCObjectLayer.ITEM_MARGIN + DCObjectLayer.ITEM_PADDING;
        let y = (position.y + 1) * CELL_SIZE - DCObjectLayer.ITEM_MARGIN - DCObjectLayer.ITEM_PADDING -
            DCProgressBar.PROGRESS_BAR_WIDTH - distanceFromBottom *
            (DCProgressBar.PROGRESS_BAR_WIDTH + DCObjectLayer.PROGRESS_BAR_DISTANCE);
        let width = (CELL_SIZE - (DCObjectLayer.ITEM_MARGIN + DCObjectLayer.ITEM_PADDING) * 2) * fractionFilled;
        let height;
        let radius;

        if (width < DCProgressBar.PROGRESS_BAR_WIDTH) {
            height = width;
            radius = width / 2;
            y += (DCProgressBar.PROGRESS_BAR_WIDTH - height) / 2;
        } else {
            height = DCProgressBar.PROGRESS_BAR_WIDTH;
            radius = DCProgressBar.PROGRESS_BAR_WIDTH / 2;
        }

        shape.graphics.drawRoundRect(
            x, y, width, height, radius
        );
        container.addChild(shape);
        return shape;
    }

    /**
     * Draws an bitmap in progressbar format.
     *
     * @param position The coordinates of the grid cell in which it should be located
     * @param container The container to which it should be drawn
     * @param originBitmap The bitmap that should be drawn
     * @param distanceFromBottom The index of its vertical position, counted from the bottom (0 is the lowest position)
     * @returns {createjs.Bitmap} The drawn bitmap
     */
    public static drawProgressbarIcon(position: IGridPosition, container: createjs.Container, originBitmap: createjs.Bitmap,
                                      distanceFromBottom: number): createjs.Bitmap {
        let bitmap = originBitmap.clone();
        container.addChild(bitmap);
        bitmap.x = (position.x + 0.5) * CELL_SIZE - DCProgressBar.PROGRESS_BAR_WIDTH * 0.5;
        bitmap.y = (position.y + 1) * CELL_SIZE - DCObjectLayer.ITEM_MARGIN - DCObjectLayer.ITEM_PADDING -
            DCProgressBar.PROGRESS_BAR_WIDTH - distanceFromBottom *
            (DCProgressBar.PROGRESS_BAR_WIDTH + DCObjectLayer.PROGRESS_BAR_DISTANCE);
        return bitmap;
    }

    constructor(container: createjs.Container, backgroundColor: string,
                fillColor: string, bitmap: createjs.Bitmap, position: IGridPosition,
                indexFromBottom: number, fillRatio: number) {
        this.container = container;
        this.backgroundColor = backgroundColor;
        this.fillColor = fillColor;
        this.bitmap = bitmap;
        this.position = position;
        this.distanceFromBottom = indexFromBottom;
        this.fillRatio = fillRatio;
    }

    public draw() {
        this.backgroundRect = DCProgressBar.drawItemProgressRectangle(this.position, this.backgroundColor,
            this.container, this.distanceFromBottom, 1);
        this.fillRect = DCProgressBar.drawItemProgressRectangle(this.position, this.fillColor, this.container,
            this.distanceFromBottom, this.fillRatio);

        DCProgressBar.drawProgressbarIcon(this.position, this.container, this.bitmap, this.distanceFromBottom);
    }
}