import {connect} from "react-redux";
import {setMapScale} from "../../../actions/map";
import ZoomControlComponent from "../../../components/map/controls/ZoomControlComponent";

const mapStateToProps = state => {
    return {
        mapScale: state.map.scale,
    };
};

const mapDispatchToProps = dispatch => {
    return {
        setMapScale: scale => dispatch(setMapScale(scale)),
    };
};

const ZoomControlContainer = connect(
    mapStateToProps,
    mapDispatchToProps
)(ZoomControlComponent);

export default ZoomControlContainer;
