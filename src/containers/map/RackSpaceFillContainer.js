import {connect} from "react-redux";
import RackFillBar from "../../components/map/elements/RackFillBar";

const mapStateToProps = (state, ownProps) => {
    const machineIds = state.objects.rack[state.objects.tile[ownProps.tileId].objectId].machineIds;
    return {
        type: "space",
        fillFraction: machineIds.filter(id => id !== null).length / machineIds.length,
    };
};

const RackSpaceFillContainer = connect(
    mapStateToProps
)(RackFillBar);

export default RackSpaceFillContainer;
