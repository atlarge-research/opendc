import {connect} from "react-redux";
import MachineComponent from "../../../../components/sidebars/topology/rack/MachineComponent";

const mapStateToProps = (state, ownProps) => {
    return {
        machine: state.objects.machine[ownProps.machineId],
    };
};

const mapDispatchToProps = dispatch => {
    return {
        onClick: () => undefined, // TODO implement transition to MACHINE mode
    };
};

const MachineContainer = connect(
    mapStateToProps,
    mapDispatchToProps
)(MachineComponent);

export default MachineContainer;
