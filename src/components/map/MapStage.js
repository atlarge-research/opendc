import React from "react";
import {Group, Layer, Stage} from "react-konva";
import {Shortcuts} from "react-shortcuts";
import DatacenterContainer from "../../containers/map/DatacenterContainer";
import ObjectHoverLayer from "../../containers/map/layers/ObjectHoverLayer";
import RoomHoverLayer from "../../containers/map/layers/RoomHoverLayer";
import jQuery from "../../util/jquery";
import {NAVBAR_HEIGHT} from "../navigation/Navbar";
import Backdrop from "./elements/Backdrop";
import GridGroup from "./groups/GridGroup";
import {
    MAP_MAX_SCALE,
    MAP_MIN_SCALE,
    MAP_MOVE_PIXELS_PER_EVENT,
    MAP_SCALE_PER_EVENT,
    MAP_SIZE_IN_PIXELS
} from "./MapConstants";

class MapStage extends React.Component {
    state = {
        width: 600,
        height: 400,
        x: 0,
        y: 0,
        scale: 1,
        mouseX: 0,
        mouseY: 0
    };

    componentWillMount() {
        this.updateDimensions();
    }

    componentDidMount() {
        window.addEventListener("resize", this.updateDimensions.bind(this));
        window.addEventListener("wheel", this.updateScale.bind(this));
    }

    componentWillUnmount() {
        window.removeEventListener("resize", this.updateDimensions.bind(this));
        window.removeEventListener("wheel", this.updateScale.bind(this));
    }

    updateDimensions() {
        this.setState({width: jQuery(window).width(), height: jQuery(window).height() - NAVBAR_HEIGHT});
    }

    updateScale(e) {
        e.preventDefault();
        const mousePointsTo = {
            x: this.state.mouseX / this.state.scale - this.state.x / this.state.scale,
            y: this.state.mouseY / this.state.scale - this.state.y / this.state.scale,
        };
        const newScale = e.deltaY < 0 ? this.state.scale * MAP_SCALE_PER_EVENT : this.state.scale / MAP_SCALE_PER_EVENT;
        const boundedScale = Math.min(Math.max(MAP_MIN_SCALE, newScale), MAP_MAX_SCALE);

        const newX = -(mousePointsTo.x - this.state.mouseX / boundedScale) * boundedScale;
        const newY = -(mousePointsTo.y - this.state.mouseY / boundedScale) * boundedScale;

        this.setPositionWithBoundsCheck(newX, newY);
        this.setState({scale: boundedScale});
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
        this.setPositionWithBoundsCheck(this.state.x + deltaX, this.state.y + deltaY);
    }

    setPositionWithBoundsCheck(newX, newY) {
        const scaledMapSize = MAP_SIZE_IN_PIXELS * this.state.scale;
        const updatedPosition = {
            x: newX > 0 ? 0 :
                (newX < -scaledMapSize + this.state.width ? -scaledMapSize + this.state.width : newX),
            y: newY > 0 ? 0 :
                (newY < -scaledMapSize + this.state.height ? -scaledMapSize + this.state.height : newY)
        };

        this.setState(updatedPosition);
    }

    render() {
        return (
            <Shortcuts name="MAP" handler={this.handleShortcuts.bind(this)} targetNodeSelector="body">
                <Stage
                    ref={(stage) => {this.stage = stage;}}
                    width={this.state.width}
                    height={this.state.height}
                    onMouseMove={this.updateMousePosition.bind(this)}
                >
                    <Layer>
                        <Group x={this.state.x} y={this.state.y} scaleX={this.state.scale} scaleY={this.state.scale}>
                            <Backdrop/>
                            <DatacenterContainer/>
                            <GridGroup/>
                        </Group>
                    </Layer>
                    <RoomHoverLayer
                        mainGroupX={this.state.x}
                        mainGroupY={this.state.y}
                        mouseX={this.state.mouseX}
                        mouseY={this.state.mouseY}
                        scale={this.state.scale}
                    />
                    <ObjectHoverLayer
                        mainGroupX={this.state.x}
                        mainGroupY={this.state.y}
                        mouseX={this.state.mouseX}
                        mouseY={this.state.mouseY}
                        scale={this.state.scale}
                    />
                </Stage>
            </Shortcuts>
        )
    }
}

export default MapStage;
