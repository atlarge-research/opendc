import PropTypes from "prop-types";
import React from "react";
import {Group, Rect} from "react-konva";
import {TILE_SIZE_IN_PIXELS} from "../MapConstants";

const MapTile = () => (
    <Group>
        <Rect
            x={this.props.tileX * TILE_SIZE_IN_PIXELS}
            y={this.props.tileY * TILE_SIZE_IN_PIXELS}
            width={TILE_SIZE_IN_PIXELS}
            height={TILE_SIZE_IN_PIXELS}
            fill={this.props.fillColor}
        />
    </Group>
);

MapTile.propTypes = {
    tileX: PropTypes.number.isRequired,
    tileY: PropTypes.number.isRequired,
    fillColor: PropTypes.string.isRequired,
};

export default MapTile;
