import {connect} from "react-redux";
import MachineListComponent from "../../../../components/sidebars/topology/rack/MachineListComponent";

const mapStateToProps = state => {
    return {
        machineIds: state.objects.rack[state.objects.tile[state.interactionLevel.tileId].objectId].machineIds,
    };
};

const MachineListContainer = connect(
    mapStateToProps
)(MachineListComponent);

export default MachineListContainer;
