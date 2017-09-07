import {connect} from "react-redux";
import {openDeleteRackModal} from "../../../../actions/modals/topology";
import DeleteRackComponent from "../../../../components/sidebars/topology/rack/DeleteRackComponent";

const mapDispatchToProps = dispatch => {
    return {
        onClick: () => dispatch(openDeleteRackModal()),
    };
};

const DeleteRackContainer = connect(
    undefined,
    mapDispatchToProps
)(DeleteRackComponent);

export default DeleteRackContainer;
