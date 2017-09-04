import PropTypes from "prop-types";
import React from 'react';
import {Layer} from "react-konva";
import HoverTile from "../elements/HoverTile";
import {TILE_SIZE_IN_PIXELS} from "../MapConstants";

class HoverTileLayerComponent extends React.Component {
    static propTypes = {
        mouseX: PropTypes.number.isRequired,
        mouseY: PropTypes.number.isRequired,
        mainGroupX: PropTypes.number.isRequired,
        mainGroupY: PropTypes.number.isRequired,
        onClick: PropTypes.func.isRequired,
        containsRack: PropTypes.bool,
    };

    state = {
        positionX: -1,
        positionY: -1,
        validity: false,
    };

    componentDidUpdate() {
        if (this.props.currentRoomInConstruction === -1) {
            return;
        }

        const positionX = Math.floor((this.props.mouseX - this.props.mainGroupX) / TILE_SIZE_IN_PIXELS);
        const positionY = Math.floor((this.props.mouseY - this.props.mainGroupY) / TILE_SIZE_IN_PIXELS);

        if (positionX !== this.state.positionX || positionY !== this.state.positionY) {
            this.setState({positionX, positionY, validity: this.props.isValid(positionX, positionY)});
        }
    }

    render() {
        if (this.props.currentRoomInConstruction === -1) {
            return <Layer/>;
        }

        const positionX = Math.floor((this.props.mouseX - this.props.mainGroupX) / TILE_SIZE_IN_PIXELS);
        const positionY = Math.floor((this.props.mouseY - this.props.mainGroupY) / TILE_SIZE_IN_PIXELS);
        const pixelX = positionX * TILE_SIZE_IN_PIXELS + this.props.mainGroupX;
        const pixelY = positionY * TILE_SIZE_IN_PIXELS + this.props.mainGroupY;

        return (
            <Layer opacity={0.4}>
                <HoverTile
                    pixelX={pixelX} pixelY={pixelY}
                    isValid={this.state.validity} onClick={() => this.props.onClick(positionX, positionY)}
                />
            </Layer>
        );
    }
}

export default HoverTileLayerComponent;
