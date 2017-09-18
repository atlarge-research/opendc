import React from "react";
import {Group} from "react-konva";
import {RACK_BACKGROUND_COLOR} from "../../../colors/index";
import RackEnergyFillContainer from "../../../containers/map/RackEnergyFillContainer";
import RackSpaceFillContainer from "../../../containers/map/RackSpaceFillContainer";
import Shapes from "../../../shapes/index";
import {convertLoadToSimulationColor} from "../../../util/simulation-load";
import TileObject from "../elements/TileObject";

const RackGroup = ({tile, inSimulation, rackLoad}) => {
    let color = RACK_BACKGROUND_COLOR;
    if (inSimulation && rackLoad) {
        color = convertLoadToSimulationColor(rackLoad);
    }

    return (
        <Group>
            <TileObject positionX={tile.positionX} positionY={tile.positionY} color={color}/>
            {inSimulation ?
                undefined :
                <Group>
                    <RackSpaceFillContainer tileId={tile.id} positionX={tile.positionX} positionY={tile.positionY}/>
                    <RackEnergyFillContainer tileId={tile.id} positionX={tile.positionX} positionY={tile.positionY}/>
                </Group>
            }
        </Group>
    );
};

RackGroup.propTypes = {
    tile: Shapes.Tile,
};

export default RackGroup;
