import {connect} from "react-redux";
import {startNewRoomConstruction} from "../../../../actions/topology/building";
import StartNewRoomConstructionComponent from "../../../../components/sidebars/topology/building/StartNewRoomConstructionComponent";

const mapDispatchToProps = dispatch => {
    return {
        onClick: () => dispatch(startNewRoomConstruction()),
    };
};

const StartNewRoomConstructionButton = connect(
    null,
    mapDispatchToProps
)(StartNewRoomConstructionComponent);

export default StartNewRoomConstructionButton;
