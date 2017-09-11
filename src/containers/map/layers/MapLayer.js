import {connect} from "react-redux";
import MapLayerComponent from "../../../components/map/layers/MapLayerComponent";

const mapStateToProps = state => {
    return {
        mapPosition: state.map.position,
        mapScale: state.map.scale,
    };
};

const MapLayer = connect(
    mapStateToProps
)(MapLayerComponent);

export default MapLayer;
