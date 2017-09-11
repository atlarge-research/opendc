import {connect} from "react-redux";
import {setMapDimensions, setMapPosition, setMapScale} from "../../actions/map";
import MapStageComponent from "../../components/map/MapStageComponent";

const mapStateToProps = state => {
    return {
        mapPosition: state.map.position,
        mapDimensions: state.map.dimensions,
        mapScale: state.map.scale,
    };
};

const mapDispatchToProps = dispatch => {
    return {
        setMapPosition: (x, y) => dispatch(setMapPosition(x, y)),
        setMapDimensions: (width, height) => dispatch(setMapDimensions(width, height)),
        setMapScale: (scale) => dispatch(setMapScale(scale)),
    };
};

const MapStage = connect(
    mapStateToProps,
    mapDispatchToProps
)(MapStageComponent);

export default MapStage;
