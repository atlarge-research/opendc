import { connect } from 'react-redux'
import { zoomInOnCenter } from '../../../../actions/map'
import ZoomControlComponent from '../../../../components/app/map/controls/ZoomControlComponent'

const mapStateToProps = state => {
    return {
        mapScale: state.map.scale,
    }
}

const mapDispatchToProps = dispatch => {
    return {
        zoomInOnCenter: zoomIn => dispatch(zoomInOnCenter(zoomIn)),
    }
}

const ZoomControlContainer = connect(mapStateToProps, mapDispatchToProps)(
    ZoomControlComponent,
)

export default ZoomControlContainer
