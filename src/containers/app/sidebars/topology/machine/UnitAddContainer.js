import {connect} from "react-redux";
import {addUnit} from "../../../../../actions/topology/machine";
import UnitAddComponent from "../../../../../components/app/sidebars/topology/machine/UnitAddComponent";

const mapStateToProps = (state, ownProps) => {
    return {
        units: Object.values(state.objects[ownProps.unitType]),
    };
};

const mapDispatchToProps = (dispatch, ownProps) => {
    return {
        onAdd: (id) => dispatch(addUnit(ownProps.unitType, id)),
    };
};

const UnitAddContainer = connect(
    mapStateToProps,
    mapDispatchToProps
)(UnitAddComponent);

export default UnitAddContainer;
