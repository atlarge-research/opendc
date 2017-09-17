import {connect} from "react-redux";
import MachineSidebarComponent from "../../../../components/sidebars/topology/machine/MachineSidebarComponent";

const mapStateToProps = state => {
    return {
        machineId: state.interactionLevel.machineId,
        inSimulation: state.currentExperimentId !== -1,
    };
};

const MachineSidebarContainer = connect(
    mapStateToProps
)(MachineSidebarComponent);

export default MachineSidebarContainer;
