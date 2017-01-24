/**
 * Interface for a subview, representing a layer of the map view.
 */
export interface Layer {
    container: createjs.Container;

    draw(): void;
}
