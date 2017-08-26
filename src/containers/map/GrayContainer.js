import {connect} from "react-redux";
import {goDownOneInteractionLevel} from "../../actions/interaction-level";
import GrayLayer from "../../components/map/elements/GrayLayer";

const mapDispatchToProps = dispatch => {
    return {
        onClick: () => dispatch(goDownOneInteractionLevel())
    };
};

const GrayContainer = connect(
    undefined,
    mapDispatchToProps
)(GrayLayer);

export default GrayContainer;
