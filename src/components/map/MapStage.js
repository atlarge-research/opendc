import React from "react";
import {Group, Layer, Stage} from "react-konva";
import {Shortcuts} from "react-shortcuts";
import DatacenterContainer from "../../containers/map/DatacenterContainer";
import HoverTileLayer from "../../containers/map/layers/HoverTileLayer";
import jQuery from "../../util/jquery";
import {NAVBAR_HEIGHT} from "../navigation/Navbar";
import Backdrop from "./elements/Backdrop";
import GridGroup from "./groups/GridGroup";
import {MAP_MOVE_PIXELS_PER_EVENT, MAP_SIZE_IN_PIXELS} from "./MapConstants";

class MapStage extends React.Component {
    state = {
        width: 600,
        height: 400,
        x: 0,
        y: 0,
        mouseX: 0,
        mouseY: 0
    };

    componentWillMount() {
        this.updateDimensions();
    }

    componentDidMount() {
        window.addEventListener("resize", this.updateDimensions.bind(this));
    }

    componentWillUnmount() {
        window.removeEventListener("resize", this.updateDimensions.bind(this));
    }

    updateDimensions() {
        this.setState({width: jQuery(window).width(), height: jQuery(window).height() - NAVBAR_HEIGHT});
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
        const updatedPosition = {
            x: this.state.x + deltaX > 0 ? 0 :
                (this.state.x + deltaX < -MAP_SIZE_IN_PIXELS + this.state.width
                    ? -MAP_SIZE_IN_PIXELS + this.state.width : this.state.x + deltaX),
            y: this.state.y + deltaY > 0 ? 0 :
                (this.state.y + deltaY < -MAP_SIZE_IN_PIXELS + this.state.height
                    ? -MAP_SIZE_IN_PIXELS + this.state.height : this.state.y + deltaY)
        };

        this.setState(updatedPosition);

        return updatedPosition;
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
                        <Group x={this.state.x} y={this.state.y}>
                            <Backdrop/>
                            <DatacenterContainer/>
                            <GridGroup/>
                        </Group>
                    </Layer>
                    <HoverTileLayer
                        mainGroupX={this.state.x}
                        mainGroupY={this.state.y}
                        mouseX={this.state.mouseX}
                        mouseY={this.state.mouseY}
                    />
                </Stage>
            </Shortcuts>
        )
    }
}

export default MapStage;
