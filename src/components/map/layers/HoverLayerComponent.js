import PropTypes from "prop-types";
import React from 'react';
import {Layer} from "react-konva";
import HoverTile from "../elements/HoverTile";
import {TILE_SIZE_IN_PIXELS} from "../MapConstants";

class HoverLayerComponent extends React.Component {
    static propTypes = {
        mouseX: PropTypes.number.isRequired,
        mouseY: PropTypes.number.isRequired,
        mainGroupX: PropTypes.number.isRequired,
        mainGroupY: PropTypes.number.isRequired,
        isEnabled: PropTypes.func.isRequired,
        onClick: PropTypes.func.isRequired,
    };

    state = {
        positionX: -1,
        positionY: -1,
        validity: false,
    };

    componentDidUpdate() {
        if (!this.props.isEnabled()) {
            return;
        }

        const positionX = Math.floor((this.props.mouseX - this.props.mainGroupX) / TILE_SIZE_IN_PIXELS);
        const positionY = Math.floor((this.props.mouseY - this.props.mainGroupY) / TILE_SIZE_IN_PIXELS);

        if (positionX !== this.state.positionX || positionY !== this.state.positionY) {
            this.setState({positionX, positionY, validity: this.props.isValid(positionX, positionY)});
        }
    }

    render() {
        if (!this.props.isEnabled()) {
            return <Layer/>;
        }

        const positionX = Math.floor((this.props.mouseX - this.props.mainGroupX) / TILE_SIZE_IN_PIXELS);
        const positionY = Math.floor((this.props.mouseY - this.props.mainGroupY) / TILE_SIZE_IN_PIXELS);
        const pixelX = positionX * TILE_SIZE_IN_PIXELS + this.props.mainGroupX;
        const pixelY = positionY * TILE_SIZE_IN_PIXELS + this.props.mainGroupY;

        return (
            <Layer opacity={0.6}>
                <HoverTile
                    pixelX={pixelX} pixelY={pixelY}
                    isValid={this.state.validity}
                    onClick={() => this.state.validity ? this.props.onClick(positionX, positionY) : undefined}
                />
                {this.props.children ? React.cloneElement(this.props.children, {pixelX, pixelY}) : undefined}
            </Layer>
        );
    }
}

export default HoverLayerComponent;
