import React from 'react'
import TilePlusIcon from '../elements/TilePlusIcon'
import HoverLayerComponent from './HoverLayerComponent'

const ObjectHoverLayerComponent = props => (
    <HoverLayerComponent {...props}>
        <TilePlusIcon {...props} />
    </HoverLayerComponent>
)

export default ObjectHoverLayerComponent
