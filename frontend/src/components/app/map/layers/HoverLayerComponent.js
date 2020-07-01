import PropTypes from 'prop-types'
import React from 'react'
import { Layer } from 'react-konva'
import HoverTile from '../elements/HoverTile'
import { TILE_SIZE_IN_PIXELS } from '../MapConstants'

class HoverLayerComponent extends React.Component {
    static propTypes = {
        mouseX: PropTypes.number.isRequired,
        mouseY: PropTypes.number.isRequired,
        mapPosition: PropTypes.object.isRequired,
        mapScale: PropTypes.number.isRequired,
        isEnabled: PropTypes.func.isRequired,
        onClick: PropTypes.func.isRequired,
    }

    state = {
        positionX: -1,
        positionY: -1,
        validity: false,
    }

    componentDidUpdate() {
        if (!this.props.isEnabled()) {
            return
        }

        const positionX = Math.floor(
            (this.props.mouseX - this.props.mapPosition.x) /
            (this.props.mapScale * TILE_SIZE_IN_PIXELS),
        )
        const positionY = Math.floor(
            (this.props.mouseY - this.props.mapPosition.y) /
            (this.props.mapScale * TILE_SIZE_IN_PIXELS),
        )

        if (
            positionX !== this.state.positionX ||
            positionY !== this.state.positionY
        ) {
            this.setState({
                positionX,
                positionY,
                validity: this.props.isValid(positionX, positionY),
            })
        }
    }

    render() {
        if (!this.props.isEnabled()) {
            return <Layer/>
        }

        const pixelX =
            this.props.mapScale * this.state.positionX * TILE_SIZE_IN_PIXELS +
            this.props.mapPosition.x
        const pixelY =
            this.props.mapScale * this.state.positionY * TILE_SIZE_IN_PIXELS +
            this.props.mapPosition.y

        return (
            <Layer opacity={0.6}>
                <HoverTile
                    pixelX={pixelX}
                    pixelY={pixelY}
                    scale={this.props.mapScale}
                    isValid={this.state.validity}
                    onClick={() =>
                        this.state.validity
                            ? this.props.onClick(this.state.positionX, this.state.positionY)
                            : undefined}
                />
                {this.props.children
                    ? React.cloneElement(this.props.children, {
                        pixelX,
                        pixelY,
                        scale: this.props.mapScale,
                    })
                    : undefined}
            </Layer>
        )
    }
}

export default HoverLayerComponent
