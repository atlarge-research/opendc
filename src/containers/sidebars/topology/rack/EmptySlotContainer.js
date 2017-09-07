import {connect} from "react-redux";
import {addMachine} from "../../../../actions/topology/rack";
import EmptySlotComponent from "../../../../components/sidebars/topology/rack/EmptySlotComponent";

const mapDispatchToProps = (dispatch, ownProps) => {
    return {
        onAdd: () => dispatch(addMachine(ownProps.position)),
    };
};

const EmptySlotContainer = connect(
    undefined,
    mapDispatchToProps
)(EmptySlotComponent);

export default EmptySlotContainer;
