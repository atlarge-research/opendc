import React from "react";
import {Stage} from "react-konva";
import {Shortcuts} from "react-shortcuts";
import MapLayer from "../../../containers/app/map/layers/MapLayer";
import ObjectHoverLayer from "../../../containers/app/map/layers/ObjectHoverLayer";
import RoomHoverLayer from "../../../containers/app/map/layers/RoomHoverLayer";
import jQuery from "../../../util/jquery";
import {NAVBAR_HEIGHT} from "../../navigation/Navbar";
import {
    MAP_MAX_SCALE,
    MAP_MIN_SCALE,
    MAP_MOVE_PIXELS_PER_EVENT,
    MAP_SCALE_PER_EVENT,
    MAP_SIZE_IN_PIXELS
} from "./MapConstants";

class MapStageComponent extends React.Component {
    state = {
        mouseX: 0,
        mouseY: 0
    };

    constructor() {
        super();

        this.updateDimensions = this.updateDimensions.bind(this);
        this.updateScale = this.updateScale.bind(this);
    }

    componentWillMount() {
        this.updateDimensions();
    }

    componentDidMount() {
        window.addEventListener("resize", this.updateDimensions);
        window.addEventListener("wheel", this.updateScale);

        window["exportCanvasToImage"] = () => {
            const canvasData = this.stage.getStage().toDataURL();
            const newWindow = window.open('about:blank', 'OpenDC Canvas Export');
            newWindow.document.write("<img src='" + canvasData + "' alt='Canvas Image Export'/>");
            newWindow.document.title = "OpenDC Canvas Export";
        }
    }

    componentWillUnmount() {
        window.removeEventListener("resize", this.updateDimensions);
        window.removeEventListener("wheel", this.updateScale);
    }

    updateDimensions() {
        this.props.setMapDimensions(jQuery(window).width(), jQuery(window).height() - NAVBAR_HEIGHT);
    }

    updateScale(e) {
        e.preventDefault();
        const mousePointsTo = {
            x: this.state.mouseX / this.props.mapScale - this.props.mapPosition.x / this.props.mapScale,
            y: this.state.mouseY / this.props.mapScale - this.props.mapPosition.y / this.props.mapScale,
        };
        const newScale = e.deltaY < 0 ? this.props.mapScale * MAP_SCALE_PER_EVENT : this.props.mapScale / MAP_SCALE_PER_EVENT;
        const boundedScale = Math.min(Math.max(MAP_MIN_SCALE, newScale), MAP_MAX_SCALE);

        const newX = -(mousePointsTo.x - this.state.mouseX / boundedScale) * boundedScale;
        const newY = -(mousePointsTo.y - this.state.mouseY / boundedScale) * boundedScale;

        this.setPositionWithBoundsCheck(newX, newY);
        this.props.setMapScale(boundedScale);
    }

    updateMousePosition() {
        const mousePos = this.stage.getStage().getPointerPosition();
        this.setState({mouseX: mousePos.x, mouseY: mousePos.y});
    }

    handleShortcuts(action) {
        switch (action) {
            case "MOVE_LEFT":
                this.moveWithDelta(MAP_MOVE_PIXELS_PER_EVENT, 0);
                break;
            case "MOVE_RIGHT":
                this.moveWithDelta(-MAP_MOVE_PIXELS_PER_EVENT, 0);
                break;
            case "MOVE_UP":
                this.moveWithDelta(0, MAP_MOVE_PIXELS_PER_EVENT);
                break;
            case "MOVE_DOWN":
                this.moveWithDelta(0, -MAP_MOVE_PIXELS_PER_EVENT);
                break;
            default:
                break;
        }
    }

    moveWithDelta(deltaX, deltaY) {
        this.setPositionWithBoundsCheck(this.props.mapPosition.x + deltaX, this.props.mapPosition.y + deltaY);
    }

    setPositionWithBoundsCheck(newX, newY) {
        const scaledMapSize = MAP_SIZE_IN_PIXELS * this.props.mapScale;
        const updatedX = newX > 0 ? 0 :
            (newX < -scaledMapSize + this.props.mapDimensions.width
                ? -scaledMapSize + this.props.mapDimensions.width : newX);
        const updatedY = newY > 0 ? 0 :
            (newY < -scaledMapSize + this.props.mapDimensions.height
                ? -scaledMapSize + this.props.mapDimensions.height : newY);

        this.props.setMapPosition(updatedX, updatedY);
    }

    render() {
        return (
            <Shortcuts name="MAP" handler={this.handleShortcuts.bind(this)} targetNodeSelector="body">
                <Stage
                    ref={(stage) => {this.stage = stage;}}
                    width={this.props.mapDimensions.width}
                    height={this.props.mapDimensions.height}
                    onMouseMove={this.updateMousePosition.bind(this)}
                >
                    <MapLayer/>
                    <RoomHoverLayer
                        mouseX={this.state.mouseX}
                        mouseY={this.state.mouseY}
                    />
                    <ObjectHoverLayer
                        mouseX={this.state.mouseX}
                        mouseY={this.state.mouseY}
                    />
                </Stage>
            </Shortcuts>
        )
    }
}

export default MapStageComponent;
