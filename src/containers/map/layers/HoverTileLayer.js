import {connect} from "react-redux";
import {toggleTileAtLocation} from "../../../actions/topology";
import HoverTileLayerComponent from "../../../components/map/layers/HoverTileLayerComponent";

const mapStateToProps = state => {
    return {
        currentRoomInConstruction: state.currentRoomInConstruction,
        isValid: (x, y) => true, // TODO implement proper validation
    };
};

const mapDispatchToProps = dispatch => {
    return {
        onClick: (x, y) => dispatch(toggleTileAtLocation(x, y)),
    };
};

const HoverTileLayer = connect(
    mapStateToProps,
    mapDispatchToProps
)(HoverTileLayerComponent);

export default HoverTileLayer;
