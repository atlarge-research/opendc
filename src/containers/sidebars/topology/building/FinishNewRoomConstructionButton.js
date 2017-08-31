import {connect} from "react-redux";
import {finishNewRoomConstruction} from "../../../../actions/topology";
import FinishNewRoomConstructionComponent from "../../../../components/sidebars/topology/building/FinishNewRoomConstructionComponent";

const mapDispatchToProps = dispatch => {
    return {
        onClick: () => dispatch(finishNewRoomConstruction()),
    };
};

const FinishNewRoomConstructionButton = connect(
    null,
    mapDispatchToProps
)(FinishNewRoomConstructionComponent);

export default FinishNewRoomConstructionButton;
