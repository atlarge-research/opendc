import React from 'react'
import { Stage } from 'react-konva'
import { Shortcuts } from 'react-shortcuts'
import MapLayer from '../../../containers/app/map/layers/MapLayer'
import ObjectHoverLayer from '../../../containers/app/map/layers/ObjectHoverLayer'
import RoomHoverLayer from '../../../containers/app/map/layers/RoomHoverLayer'
import { NAVBAR_HEIGHT } from '../../navigation/Navbar'
import { MAP_MOVE_PIXELS_PER_EVENT } from './MapConstants'
import { Provider } from 'react-redux'
import { store } from '../../../store/configure-store'

class MapStageComponent extends React.Component {
    state = {
        mouseX: 0,
        mouseY: 0,
    }

    constructor(props) {
        super(props)

        this.updateDimensions = this.updateDimensions.bind(this)
        this.updateScale = this.updateScale.bind(this)
    }

    componentDidMount() {
        this.updateDimensions()

        window.addEventListener('resize', this.updateDimensions)
        window.addEventListener('wheel', this.updateScale)

        window['exportCanvasToImage'] = () => {
            const download = document.createElement('a')
            download.href = this.stage.getStage().toDataURL()
            download.download = 'opendc-canvas-export-' + Date.now() + '.png'
            download.click()
        }
    }

    componentWillUnmount() {
        window.removeEventListener('resize', this.updateDimensions)
        window.removeEventListener('wheel', this.updateScale)
    }

    updateDimensions() {
        this.props.setMapDimensions(window.innerWidth, window.innerHeight - NAVBAR_HEIGHT)
    }

    updateScale(e) {
        e.preventDefault()
        this.props.zoomInOnPosition(e.deltaY < 0, this.state.mouseX, this.state.mouseY)
    }

    updateMousePosition() {
        const mousePos = this.stage.getStage().getPointerPosition()
        this.setState({ mouseX: mousePos.x, mouseY: mousePos.y })
    }

    handleShortcuts(action) {
        switch (action) {
            case 'MOVE_LEFT':
                this.moveWithDelta(MAP_MOVE_PIXELS_PER_EVENT, 0)
                break
            case 'MOVE_RIGHT':
                this.moveWithDelta(-MAP_MOVE_PIXELS_PER_EVENT, 0)
                break
            case 'MOVE_UP':
                this.moveWithDelta(0, MAP_MOVE_PIXELS_PER_EVENT)
                break
            case 'MOVE_DOWN':
                this.moveWithDelta(0, -MAP_MOVE_PIXELS_PER_EVENT)
                break
            default:
                break
        }
    }

    moveWithDelta(deltaX, deltaY) {
        this.props.setMapPositionWithBoundsCheck(this.props.mapPosition.x + deltaX, this.props.mapPosition.y + deltaY)
    }

    render() {
        return (
            <Shortcuts name="MAP" handler={this.handleShortcuts.bind(this)} targetNodeSelector="body">
                <Stage
                    ref={(stage) => {
                        this.stage = stage
                    }}
                    width={this.props.mapDimensions.width}
                    height={this.props.mapDimensions.height}
                    onMouseMove={this.updateMousePosition.bind(this)}
                >
                    <Provider store={store}>
                        <MapLayer />
                        <RoomHoverLayer mouseX={this.state.mouseX} mouseY={this.state.mouseY} />
                        <ObjectHoverLayer mouseX={this.state.mouseX} mouseY={this.state.mouseY} />
                    </Provider>
                </Stage>
            </Shortcuts>
        )
    }
}

export default MapStageComponent
