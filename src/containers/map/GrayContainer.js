import {connect} from "react-redux";
import {goFromRoomToBuilding} from "../../actions/interaction-level";
import GrayLayer from "../../components/map/elements/GrayLayer";

const mapDispatchToProps = dispatch => {
    return {
        onClick: () => dispatch(goFromRoomToBuilding())
    };
};

const GrayContainer = connect(
    undefined,
    mapDispatchToProps
)(GrayLayer);

export default GrayContainer;
