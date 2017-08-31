import React from "react";
import {Group, Layer, Stage} from "react-konva";
import DatacenterContainer from "../../containers/map/DatacenterContainer";
import HoverTileLayer from "../../containers/map/layers/HoverTileLayer";
import jQuery from "../../util/jquery";
import {NAVBAR_HEIGHT} from "../navigation/Navbar";
import Backdrop from "./elements/Backdrop";
import GridGroup from "./groups/GridGroup";
import {MAP_SIZE_IN_PIXELS} from "./MapConstants";

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

    dragBoundFunc(pos) {
        const updatedPosition = {
            x: pos.x > 0 ? 0 :
                (pos.x < -MAP_SIZE_IN_PIXELS + this.state.width ? -MAP_SIZE_IN_PIXELS + this.state.width : pos.x),
            y: pos.y > 0 ? 0 :
                (pos.y < -MAP_SIZE_IN_PIXELS + this.state.height ? -MAP_SIZE_IN_PIXELS + this.state.height : pos.y)
        };

        this.setState(updatedPosition);

        return updatedPosition;
    }

    render() {
        return (
            <Stage ref={(stage) => {this.stage = stage;}}
                   width={this.state.width}
                   height={this.state.height}
                   onMouseMove={this.updateMousePosition.bind(this)}>
                <Layer>
                    <Group draggable={true} dragBoundFunc={this.dragBoundFunc.bind(this)}>
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
        )
    }
}

export default MapStage;
