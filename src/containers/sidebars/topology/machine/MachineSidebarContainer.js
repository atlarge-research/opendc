import {connect} from "react-redux";
import MachineSidebarComponent from "../../../../components/sidebars/topology/machine/MachineSidebarComponent";

const mapStateToProps = state => {
    return {
        inSimulation: state.currentExperimentId !== -1,
        machineId: state.objects.rack[state.objects.tile[state.interactionLevel.tileId].objectId]
            .machineIds[state.interactionLevel.position - 1],
    };
};

const MachineSidebarContainer = connect(
    mapStateToProps
)(MachineSidebarComponent);

export default MachineSidebarContainer;
