import {connect} from "react-redux";
import {goDownOneInteractionLevel} from "../../../../../actions/interaction-level";
import BackToRackComponent from "../../../../../components/app/sidebars/topology/machine/BackToRackComponent";

const mapDispatchToProps = dispatch => {
    return {
        onClick: () => dispatch(goDownOneInteractionLevel()),
    };
};

const BackToRackContainer = connect(
    undefined,
    mapDispatchToProps
)(BackToRackComponent);

export default BackToRackContainer;
