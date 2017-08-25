import React from "react";
import {Group, Layer, Stage} from "react-konva";
import DatacenterContainer from "../../containers/map/DatacenterContainer";
import jQuery from "../../util/jquery";
import Backdrop from "./elements/Backdrop";
import GridGroup from "./groups/GridGroup";
import {MAP_SIZE_IN_PIXELS} from "./MapConstants";

class MapStage extends React.Component {
    state = {
        width: 600,
        height: 400
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
        this.setState({width: jQuery(window).width(), height: jQuery(window).height()});
    }

    dragBoundFunc(pos) {
        return {
            x: pos.x > 0 ? 0 :
                (pos.x < -MAP_SIZE_IN_PIXELS + this.state.width ? -MAP_SIZE_IN_PIXELS + this.state.width : pos.x),
            y: pos.y > 0 ? 0 :
                (pos.y < -MAP_SIZE_IN_PIXELS + this.state.height ? -MAP_SIZE_IN_PIXELS + this.state.height : pos.y)
        }
    }

    render() {
        return (
            <Stage width={this.state.width} height={this.state.height}>
                <Layer>
                    <Group draggable={true} dragBoundFunc={this.dragBoundFunc.bind(this)}>
                        <Backdrop/>
                        <DatacenterContainer/>
                        <GridGroup/>
                    </Group>
                </Layer>
            </Stage>
        )
    }
}

export default MapStage;
