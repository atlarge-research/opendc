import { connect } from 'react-redux'
import { setMapDimensions, setMapPositionWithBoundsCheck, zoomInOnPosition } from '../../../actions/map'
import MapStageComponent from '../../../components/app/map/MapStageComponent'

const mapStateToProps = (state) => {
    return {
        mapPosition: state.map.position,
        mapDimensions: state.map.dimensions,
    }
}

const mapDispatchToProps = (dispatch) => {
    return {
        zoomInOnPosition: (zoomIn, x, y) => dispatch(zoomInOnPosition(zoomIn, x, y)),
        setMapPositionWithBoundsCheck: (x, y) => dispatch(setMapPositionWithBoundsCheck(x, y)),
        setMapDimensions: (width, height) => dispatch(setMapDimensions(width, height)),
    }
}

const MapStage = connect(mapStateToProps, mapDispatchToProps)(MapStageComponent)

export default MapStage
