import {connect} from "react-redux";
import {goDownOneInteractionLevel} from "../../../../../actions/interaction-level";
import BackToRoomComponent from "../../../../../components/app/sidebars/topology/rack/BackToRoomComponent";

const mapDispatchToProps = dispatch => {
    return {
        onClick: () => dispatch(goDownOneInteractionLevel()),
    };
};

const BackToRoomContainer = connect(
    undefined,
    mapDispatchToProps
)(BackToRoomComponent);

export default BackToRoomContainer;
