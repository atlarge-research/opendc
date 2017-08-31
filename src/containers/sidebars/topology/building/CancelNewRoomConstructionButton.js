import {connect} from "react-redux";
import {cancelNewRoomConstruction} from "../../../../actions/topology";
import CancelNewRoomConstructionComponent from "../../../../components/sidebars/topology/building/CancelNewRoomConstructionComponent";

const mapDispatchToProps = dispatch => {
    return {
        onClick: () => dispatch(cancelNewRoomConstruction()),
    };
};

const CancelNewRoomConstructionButton = connect(
    null,
    mapDispatchToProps
)(CancelNewRoomConstructionComponent);

export default CancelNewRoomConstructionButton;
