import {connect} from "react-redux";
import {startObjectConstruction, stopObjectConstruction} from "../../../../actions/topology";
import RackConstructionComponent from "../../../../components/sidebars/topology/room/RackConstructionComponent";

const mapStateToProps = state => {
    return {
        inObjectConstructionMode: state.construction.inObjectConstructionMode,
    };
};

const mapDispatchToProps = dispatch => {
    return {
        onStart: () => dispatch(startObjectConstruction()),
        onStop: () => dispatch(stopObjectConstruction()),
    };
};

const RackConstructionContainer = connect(
    mapStateToProps,
    mapDispatchToProps
)(RackConstructionComponent);

export default RackConstructionContainer;
