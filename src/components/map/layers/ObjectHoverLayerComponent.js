import React from 'react';
import TilePlusIcon from "../elements/TilePlusIcon";
import HoverLayerComponent from "./HoverLayerComponent";

const ObjectHoverLayerComponent = (props) => (
    <HoverLayerComponent {...props}>
        <TilePlusIcon/>
    </HoverLayerComponent>
);

export default ObjectHoverLayerComponent;
